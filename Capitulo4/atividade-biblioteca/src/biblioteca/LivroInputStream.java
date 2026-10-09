package biblioteca;

import java.io.*;

public class LivroInputStream extends InputStream {
    private final DataInputStream origem;
    public LivroInputStream(InputStream origem) { this.origem = new DataInputStream(origem); }
    public Livro[] lerLivros() throws IOException {
        if (origem.readInt() != LivroOutputStream.MAGIC) throw new IOException("Cabeçalho inválido");
        int n = origem.readInt();
        if (n < 0 || n > 10000) throw new IOException("Quantidade inválida");
        Livro[] livros = new Livro[n];
        for (int i = 0; i < n; i++) livros[i] = lerLivro(origem);
        return livros;
    }
    static Livro lerLivro(DataInputStream in) throws IOException {
        return new Livro(in.readInt(), in.readUTF(), in.readUTF());
    }
    @Override public int read() throws IOException { return origem.read(); }
    @Override public int read(byte[] b, int off, int len) throws IOException { return origem.read(b, off, len); }
    @Override public void close() throws IOException { origem.close(); }
}
