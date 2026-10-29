package biblioteca;
import java.util.*;
/** Codec JSON para objetos cujos campos são strings. Rejeita entrada fora desse formato. */
public final class Json {
    public static String encode(Map<String,String> fields) {
        StringJoiner out = new StringJoiner(",", "{", "}");
        fields.forEach((k,v) -> out.add(quote(k)+":"+quote(v)));
        return out.toString();
    }
    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch(c) {
                case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n"); case '\r' -> b.append("\\r"); case '\t' -> b.append("\\t");
                default -> { if(c < 32) b.append(String.format("\\u%04x",(int)c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }
    public static Map<String,String> decode(String text) {
        class Parser {
            int i;
            void space() { while(i<text.length() && Character.isWhitespace(text.charAt(i))) i++; }
            void expect(char c) { space(); if(i>=text.length() || text.charAt(i++)!=c) throw new IllegalArgumentException("JSON inválido"); }
            String string() {
                expect('"'); StringBuilder b=new StringBuilder();
                while(i<text.length()) {
                    char c=text.charAt(i++); if(c=='"') return b.toString();
                    if(c=='\\') {
                        if(i>=text.length()) throw new IllegalArgumentException("Escape incompleto");
                        c=text.charAt(i++);
                        switch(c) {
                            case '"','\\','/' -> b.append(c);
                            case 'n' -> b.append('\n'); case 'r' -> b.append('\r'); case 't' -> b.append('\t');
                            case 'b' -> b.append('\b'); case 'f' -> b.append('\f');
                            case 'u' -> { b.append((char)Integer.parseInt(text.substring(i,i+4),16)); i+=4; }
                            default -> throw new IllegalArgumentException("Escape inválido");
                        }
                    } else { if(c<32) throw new IllegalArgumentException("Controle inválido"); b.append(c); }
                }
                throw new IllegalArgumentException("String incompleta");
            }
        }
        Parser s=new Parser(); Map<String,String> result=new LinkedHashMap<>(); s.expect('{'); s.space();
        if(s.i<text.length() && text.charAt(s.i)=='}') s.i++;
        else while(true) {
            String k=s.string(); s.expect(':'); String v=s.string();
            if(result.putIfAbsent(k,v)!=null) throw new IllegalArgumentException("Campo repetido");
            s.space(); if(s.i<text.length() && text.charAt(s.i)=='}') { s.i++; break; } s.expect(',');
        }
        s.space(); if(s.i!=text.length()) throw new IllegalArgumentException("Dados após JSON"); return result;
    }
    public static String required(Map<String,String> m,String key) {
        String v=m.get(key); if(v==null || v.isBlank()) throw new IllegalArgumentException("Campo obrigatório: "+key); return v;
    }
}
