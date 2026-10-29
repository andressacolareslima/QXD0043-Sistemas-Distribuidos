package biblioteca;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.*;

public final class Servidor extends UnicastRemoteObject implements BibliotecaRemota {
    private final Canal canal=new Canal();
    private final Set<Sessao> sessoes=ConcurrentHashMap.newKeySet();
    private final ExecutorService entregas=Executors.newCachedThreadPool();
    private final int portaServico;
    private Servidor(int portaServico) throws RemoteException { super(portaServico); this.portaServico=portaServico; }
    public ProtocoloRemoto autenticar(String json,NotificadorRemoto callback) throws RemoteException {
        var c=Json.decode(json); String login=Json.required(c,"login"), senha=Json.required(c,"senha");
        if(!"1234".equals(senha) || !(login.equals("aluno") || login.equals("admin")))
            throw new RemoteException("Credenciais inválidas");
        if(callback==null) throw new RemoteException("Callback obrigatório");
        Usuario u=login.equals("admin") ? new Funcionario(login) : new Aluno(login);
        Sessao s=new Sessao(u,callback); sessoes.add(s);
        System.out.println("Autenticado: "+login); return s;
    }
    private final class Sessao extends UnicastRemoteObject implements ProtocoloRemoto {
        private final Usuario usuario;
        private final NotificadorRemoto callback;
        private boolean ativa=true;
        Sessao(Usuario usuario,NotificadorRemoto callback) throws RemoteException {
            super(portaServico); this.usuario=usuario; this.callback=callback;
        }
        // RMI entrega a chamada e correlaciona o retorno; o envelope preserva o requestId.
        private Map<String,String> getRequest(String json) { return Json.decode(json); }
        private String sendReply(String id,String status,String resultado) {
            return Json.encode(Map.of("messageType","REPLY","requestId",id,"status",status,"result",resultado));
        }
        public synchronized String doOperation(String json) {
            String id="desconhecido";
            try {
                var req=getRequest(json); id=Json.required(req,"requestId");
                if(!ativa) throw new IllegalArgumentException("Sessão encerrada");
                if(!"REQUEST".equals(Json.required(req,"messageType"))) throw new IllegalArgumentException("Esperado REQUEST");
                if(!"Biblioteca".equals(Json.required(req,"objectReference"))) throw new IllegalArgumentException("Objeto desconhecido");
                String metodo=Json.required(req,"methodId"); String args=Json.required(req,"arguments");
                System.out.println("REQUEST "+id+" methodId="+metodo);
                String resultado=switch(metodo) {
                    case "perfil" -> Json.encode(Map.of("nome",usuario.getNome(),"perfil",usuario.getPerfil()));
                    case "historico" -> canal.historicoJson();
                    case "publicar" -> {
                        if(!(usuario instanceof Funcionario)) throw new IllegalArgumentException("Publicação exige funcionário");
                        Notificacao recebida=Notificacao.fromJson(args); // Novo objeto local, reconstruído por valor.
                        var dados=Json.decode(recebida.toJson());
                        Notificacao n=new Notificacao(dados.get("tipo"),dados.get("mensagem"),System.currentTimeMillis(),usuario);
                        canal.adicionar(n); String payload=n.toJson();
                        for(Sessao s:sessoes) entregas.submit(() -> {
                            try { s.callback.receber(payload); }
                            catch(RemoteException e) { sessoes.remove(s); System.err.println("Callback indisponível: "+s.usuario.getNome()); }
                        });
                        yield payload;
                    }
                    case "sair" -> { ativa=false; sessoes.remove(this); yield "Sessão encerrada"; }
                    default -> throw new IllegalArgumentException("Método desconhecido: "+metodo);
                };
                return sendReply(id,"OK",resultado);
            } catch(RuntimeException e) { return sendReply(id,"ERRO",e.getMessage()==null ? e.getClass().getSimpleName():e.getMessage()); }
        }
    }
    public static void main(String[] args) throws Exception {
        String host=args.length>0 ? args[0]:"127.0.0.1";
        int registryPort=args.length>1 ? Integer.parseInt(args[1]):1099;
        int servicePort=args.length>2 ? Integer.parseInt(args[2]):1100;
        System.setProperty("java.rmi.server.hostname",host);
        Registry registry=LocateRegistry.createRegistry(registryPort);
        Servidor servidor=new Servidor(servicePort);
        registry.rebind("Biblioteca",servidor);
        System.out.println("Biblioteca RMI pronta: "+host+":"+registryPort+"; serviço="+servicePort);
    }
}
