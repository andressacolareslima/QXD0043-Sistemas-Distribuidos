package biblioteca;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class CatalogoService {
    private final Map<Integer, Livro> livros = new ConcurrentHashMap<>();
    public CatalogoService() {
        cadastrar(new Livro(1, "Sistemas Distribuídos", "Tanenbaum"));
        cadastrar(new Livro(2, "Redes de Computadores", "Kurose"));
    }
    public void cadastrar(Livro livro) { livros.put(livro.getId(), livro); }
    public Livro buscar(int id) { return livros.get(id); }
    public Livro[] listar() {
        return livros.values().stream().sorted(java.util.Comparator.comparingInt(Livro::getId))
                .toArray(Livro[]::new);
    }
}
