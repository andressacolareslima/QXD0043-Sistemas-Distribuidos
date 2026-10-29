package biblioteca;
public abstract class Usuario {
    private final String nome;
    protected Usuario(String nome) { this.nome=nome; }
    public String getNome() { return nome; }
    public abstract String getPerfil();
}
