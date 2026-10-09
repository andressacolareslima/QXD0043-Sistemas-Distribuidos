package biblioteca;

public class AutenticacaoService {
    private final Usuario cadastrado = new Usuario("aluno", "1234");
    public boolean autenticar(Usuario usuario) {
        return cadastrado.getNome().equals(usuario.getNome())
                && cadastrado.getSenha().equals(usuario.getSenha());
    }
}
