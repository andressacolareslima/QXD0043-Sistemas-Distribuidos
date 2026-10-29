package biblioteca;
import java.util.*;
public final class Canal {
    private final List<Notificacao> historico=new ArrayList<>(); // Canal tem notificacoes.
    public synchronized void adicionar(Notificacao n) { historico.add(n); }
    public synchronized String historicoJson() {
        return "["+String.join(",",historico.stream().map(Notificacao::toJson).toList())+"]";
    }
}
