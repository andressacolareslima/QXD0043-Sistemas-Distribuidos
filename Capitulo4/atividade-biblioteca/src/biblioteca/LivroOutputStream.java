package biblioteca;

import java.io.*;
import java.util.Objects;

/** Formato: magic (int), quantidade (int), depois id (int), título e autor (UTF). */
public class LivroOutputStream extends OutputStream {
    static final int MAGIC = 0x42494231;
    private final DataOutputStream destino;
    private final Livro[] livros;
    private final int quantidade;
    public LivroOutputStream(OutputStream destino, Livro[] livros, int quantidade) {
        Objects.requireNonNull(livros);
        if (quantidade < 0 || quantidade > livros.length || quantidade > 10000)
            throw new IllegalArgumentException("Quantidade inválida");
        this.destino = new DataOutputStream(Objects.requireNonNull(destino));
        this.livros = livros; this.quantidade = quantidade;
    }
    public void enviar() throws IOException {
        destino.writeInt(MAGIC); destino.writeInt(quantidade);
        for (int i = 0; i < quantidade; i++) escreverLivro(destino, livros[i]);
        flush();
    }
    static void escreverLivro(DataOutputStream out, Livro livro) throws IOException {
        out.writeInt(livro.getId()); out.writeUTF(livro.getTitulo()); out.writeUTF(livro.getAutor());
    }
    @Override public void write(int b) throws IOException { destino.write(b); }
    @Override public void write(byte[] b, int off, int len) throws IOException { destino.write(b, off, len); }
    @Override public void flush() throws IOException { destino.flush(); }
    @Override public void close() throws IOException { destino.close(); }
}
