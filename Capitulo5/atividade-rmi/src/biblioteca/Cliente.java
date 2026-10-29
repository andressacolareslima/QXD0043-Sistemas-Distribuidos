package biblioteca;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;

public final class Cliente {
    public static String doOperation(ProtocoloRemoto remoto,String metodo,String argumentos) throws RemoteException {
        String id=UUID.randomUUID().toString();
        String request=Json.encode(Map.of("messageType","REQUEST","requestId",id,
            "objectReference","Biblioteca","methodId",metodo,"arguments",argumentos));
        String reply=remoto.doOperation(request); var m=Json.decode(reply);
        if(!id.equals(m.get("requestId")) || !"REPLY".equals(m.get("messageType")))
            throw new RemoteException("Resposta não corresponde à requisição");
        return reply;
    }
    private static final class Callback extends UnicastRemoteObject implements NotificadorRemoto {
        Callback(int port) throws RemoteException { super(port); }
        public void receber(String json) { System.out.println("\nNOTIFICAÇÃO: "+Notificacao.fromJson(json).toJson()); }
    }
    public static void main(String[] args) throws Exception {
        String host=args.length>0?args[0]:"127.0.0.1";
        String login=args.length>1?args[1]:"aluno", senha=args.length>2?args[2]:"1234";
        int registryPort=args.length>3?Integer.parseInt(args[3]):1099;
        if(args.length>4) System.setProperty("java.rmi.server.hostname",args[4]);
        int callbackPort=args.length>5?Integer.parseInt(args[5]):0;
        Callback callback=new Callback(callbackPort);
        ProtocoloRemoto sessao=null;
        try(Scanner in=new Scanner(System.in)) {
            BibliotecaRemota biblioteca=(BibliotecaRemota)LocateRegistry.getRegistry(host,registryPort).lookup("Biblioteca");
            sessao=biblioteca.autenticar(Json.encode(Map.of("login",login,"senha",senha)),callback);
            System.out.println("Autenticado como "+login+". Comandos: perfil, historico, publicar TIPO mensagem, sair");
            while(in.hasNextLine()) {
                String[] command=in.nextLine().trim().split(" +",3);
                String method=command[0], payload="{}";
                if(method.equals("publicar")) {
                    if(command.length<3) { System.out.println("Uso: publicar TIPO mensagem"); continue; }
                    payload=new Notificacao(command[1],command[2],System.currentTimeMillis(),new Funcionario(login)).toJson();
                }
                System.out.println(doOperation(sessao,method,payload));
                if(method.equals("sair")) break;
            }
        } catch(Exception e) { System.err.println("Falha: "+e.getMessage()); }
        finally {
            if(sessao!=null) try { doOperation(sessao,"sair","{}"); } catch(RemoteException ignored) { }
            UnicastRemoteObject.unexportObject(callback,true);
        }
    }
}
