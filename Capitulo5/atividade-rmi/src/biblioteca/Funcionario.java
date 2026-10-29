package biblioteca;
public final class Funcionario extends Usuario {
    public Funcionario(String nome) { super(nome); }
    public String getPerfil() { return "FUNCIONARIO"; }
}
