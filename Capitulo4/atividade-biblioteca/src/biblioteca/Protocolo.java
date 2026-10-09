package biblioteca;

import java.io.*;

/** Serialização binária explícita; ambos os lados empacotam e desempacotam. */
public final class Protocolo {
    private Protocolo() { }
    public static class Request {
        public final String operacao;
        public final Livro livro;
        public Request(String operacao, Livro livro) { this.operacao = operacao; this.livro = livro; }
    }
    public static class Reply {
        public final boolean sucesso;
        public final String mensagem;
        public final Livro livro;
        public Reply(boolean sucesso, String mensagem, Livro livro) {
            this.sucesso = sucesso; this.mensagem = mensagem; this.livro = livro;
        }
    }
    public static byte[] empacotarRequest(Request r) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeUTF(r.operacao); LivroOutputStream.escreverLivro(out, r.livro);
        return bytes.toByteArray();
    }
    public static Request desempacotarRequest(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        Request r = new Request(in.readUTF(), LivroInputStream.lerLivro(in));
        if (in.available() != 0) throw new IOException("Bytes extras no request");
        return r;
    }
    public static byte[] empacotarReply(Reply r) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeBoolean(r.sucesso); out.writeUTF(r.mensagem); out.writeBoolean(r.livro != null);
        if (r.livro != null) LivroOutputStream.escreverLivro(out, r.livro);
        return bytes.toByteArray();
    }
    public static Reply desempacotarReply(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        Reply r = new Reply(in.readBoolean(), in.readUTF(), in.readBoolean() ? LivroInputStream.lerLivro(in) : null);
        if (in.available() != 0) throw new IOException("Bytes extras no reply");
        return r;
    }
    public static void enviar(OutputStream destino, byte[] bytes) throws IOException {
        DataOutputStream out = new DataOutputStream(destino);
        out.writeInt(bytes.length); out.write(bytes); out.flush();
    }
    public static byte[] receber(InputStream origem) throws IOException {
        DataInputStream in = new DataInputStream(origem);
        int n = in.readInt();
        if (n < 1 || n > 1048576) throw new IOException("Tamanho de mensagem inválido");
        byte[] bytes = new byte[n]; in.readFully(bytes); return bytes;
    }
}
