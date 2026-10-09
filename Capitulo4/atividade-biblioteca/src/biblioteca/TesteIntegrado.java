package biblioteca;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class TesteIntegrado {
    static void verificar(boolean condicao) {
        if (!condicao) throw new AssertionError("Verificação falhou");
    }
    static void conferir(Livro[] livros) {
        verificar(livros.length == 1 && livros[0].getId() == 1
                && livros[0].getTitulo().equals("Sistemas Distribuídos")
                && livros[0].getAutor().equals("Tanenbaum"));
    }
    public static void main(String[] args) throws Exception {
        Livro[] livros = new CatalogoService().listar();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new LivroOutputStream(bytes, livros, 1).enviar();
        conferir(new LivroInputStream(new ByteArrayInputStream(bytes.toByteArray())).lerLivros());
        try {
            new LivroOutputStream(bytes, livros, 3);
            throw new AssertionError("Deveria rejeitar quantidade inválida");
        } catch (IllegalArgumentException esperado) { }
        try {
            new LivroInputStream(new ByteArrayInputStream(new byte[8])).lerLivros();
            throw new AssertionError("Deveria rejeitar cabeçalho inválido");
        } catch (IOException esperado) { }
        Path arquivo = Files.createTempFile("biblioteca-", ".bin");
        try {
            try (LivroOutputStream out = new LivroOutputStream(new FileOutputStream(arquivo.toFile()), livros, 1)) { out.enviar(); }
            try (LivroInputStream in = new LivroInputStream(new FileInputStream(arquivo.toFile()))) { conferir(in.lerLivros()); }
        } finally { Files.deleteIfExists(arquivo); }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket servidor = new ServerSocket(0)) {
            Future<?> recebido = executor.submit(() -> {
                try (Socket socket = servidor.accept()) {
                    socket.setSoTimeout(5000);
                    conferir(new LivroInputStream(socket.getInputStream()).lerLivros());
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            try (Socket socket = new Socket("localhost", servidor.getLocalPort())) {
                new LivroOutputStream(socket.getOutputStream(), livros, 1).enviar();
            }
            recebido.get(5, TimeUnit.SECONDS);
            Future<?> rpc = executor.submit(() -> {
                try (Socket socket = servidor.accept()) {
                    socket.setSoTimeout(5000);
                    Protocolo.Request request = Protocolo.desempacotarRequest(Protocolo.receber(socket.getInputStream()));
                    Protocolo.enviar(socket.getOutputStream(), Protocolo.empacotarReply(Main.executar(new CatalogoService(), request)));
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
            try (Socket socket = new Socket("localhost", servidor.getLocalPort())) {
                socket.setSoTimeout(5000);
                Protocolo.enviar(socket.getOutputStream(), Protocolo.empacotarRequest(new Protocolo.Request("CADASTRAR", livros[0])));
                Protocolo.Reply reply = Protocolo.desempacotarReply(Protocolo.receber(socket.getInputStream()));
                verificar(reply.sucesso && reply.livro.getTitulo().equals(livros[0].getTitulo()));
            }
            rpc.get(5, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
        verificar(new AutenticacaoService().autenticar(new Usuario("aluno", "1234")));
        verificar(!new AutenticacaoService().autenticar(new Usuario("aluno", "errada")));
        byte[] notificacao = Multicast.empacotar("ALERTA", "Novo livro", 123456789L);
        verificar(Multicast.desempacotar(notificacao, notificacao.length).equals("[ALERTA] Novo livro (timestamp=123456789)"));
        System.out.println("OK: streams, arquivo, TCP, request/reply, autenticação e formato multicast.");
    }
}
