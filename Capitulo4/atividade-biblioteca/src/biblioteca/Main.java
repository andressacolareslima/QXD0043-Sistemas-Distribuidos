package biblioteca;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Consulte os comandos no README.md");
        CatalogoService catalogo = new CatalogoService();
        String host = args.length > 1 ? args[1] : "localhost";
        switch (args[0]) {
            case "stdout":
                new LivroOutputStream(System.out, catalogo.listar(), 2).enviar(); break;
            case "stdin":
                mostrar(new LivroInputStream(System.in).lerLivros()); break;
            case "arquivo-escrever":
                try (LivroOutputStream out = new LivroOutputStream(new FileOutputStream(host), catalogo.listar(), 2)) { out.enviar(); }
                break;
            case "arquivo-ler":
                try (LivroInputStream in = new LivroInputStream(new FileInputStream(host))) { mostrar(in.lerLivros()); }
                break;
            case "streams-servidor":
                try (ServerSocket servidor = new ServerSocket(5000); Socket cliente = servidor.accept();
                     LivroInputStream in = new LivroInputStream(cliente.getInputStream())) { mostrar(in.lerLivros()); }
                break;
            case "streams-cliente":
                try (Socket socket = new Socket(host, 5000)) {
                    new LivroOutputStream(socket.getOutputStream(), catalogo.listar(), 2).enviar();
                } break;
            case "rpc-servidor": servidorRpc(catalogo); break;
            case "rpc-cliente":
                try (Socket socket = new Socket(host, 5001)) {
                    socket.setSoTimeout(5000);
                    Protocolo.Request request = new Protocolo.Request("CADASTRAR", new Livro(3, "Java", "Deitel"));
                    Protocolo.enviar(socket.getOutputStream(), Protocolo.empacotarRequest(request));
                    Protocolo.Reply reply = Protocolo.desempacotarReply(Protocolo.receber(socket.getInputStream()));
                    System.out.println(reply.sucesso + ": " + reply.mensagem + " / " + reply.livro);
                } break;
            case "multicast-servidor": Multicast.servidor(); break;
            case "multicast-cliente": Multicast.cliente(host, args.length > 2 ? args[2] : "aluno",
                    args.length > 3 ? args[3] : "1234", args.length > 4 ? args[4] : null); break;
            default: throw new IllegalArgumentException("Comando desconhecido: " + args[0]);
        }
    }
    static void mostrar(Livro[] livros) { for (Livro livro : livros) System.out.println(livro); }
    static Protocolo.Reply executar(CatalogoService catalogo, Protocolo.Request r) {
        switch (r.operacao) {
            case "CADASTRAR": catalogo.cadastrar(r.livro); return new Protocolo.Reply(true, "Livro cadastrado", r.livro);
            case "BUSCAR":
                Livro livro = catalogo.buscar(r.livro.getId());
                return new Protocolo.Reply(livro != null, livro != null ? "Livro encontrado" : "Livro inexistente", livro);
            default: return new Protocolo.Reply(false, "Operação desconhecida", null);
        }
    }
    static void servidorRpc(CatalogoService catalogo) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try (ServerSocket servidor = new ServerSocket(5001)) {
            System.out.println("RPC TCP na porta 5001");
            while (true) {
                Socket cliente = servidor.accept();
                pool.submit(() -> {
                    try (Socket socket = cliente) {
                        socket.setSoTimeout(5000);
                        Protocolo.Request r = Protocolo.desempacotarRequest(Protocolo.receber(socket.getInputStream()));
                        Protocolo.enviar(socket.getOutputStream(), Protocolo.empacotarReply(executar(catalogo, r)));
                    } catch (IOException e) { System.err.println(e.getMessage()); }
                });
            }
        } finally { pool.shutdownNow(); }
    }
}
