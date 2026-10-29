package biblioteca;
import java.util.Map;
public final class Notificacao {
    private final String tipo, mensagem;
    private final long timestamp;
    private final Usuario autor; // Notificacao tem um Usuario.
    public Notificacao(String tipo,String mensagem,long timestamp,Usuario autor) {
        if(!java.util.Set.of("NOTIFICACAO","ALERTA","ATUALIZACAO").contains(tipo))
            throw new IllegalArgumentException("Tipo inválido");
        this.tipo=tipo; this.mensagem=mensagem; this.timestamp=timestamp; this.autor=autor;
    }
    public String toJson() {
        return Json.encode(Map.of("tipo",tipo,"mensagem",mensagem,"timestamp",Long.toString(timestamp),
            "autor",autor.getNome(),"perfil",autor.getPerfil()));
    }
    public static Notificacao fromJson(String json) {
        var m=Json.decode(json); String perfil=Json.required(m,"perfil");
        Usuario u=switch(perfil) {
            case "ALUNO" -> new Aluno(Json.required(m,"autor"));
            case "FUNCIONARIO" -> new Funcionario(Json.required(m,"autor"));
            default -> throw new IllegalArgumentException("Perfil inválido");
        };
        return new Notificacao(Json.required(m,"tipo"),Json.required(m,"mensagem"),
            Long.parseLong(Json.required(m,"timestamp")),u);
    }
}
