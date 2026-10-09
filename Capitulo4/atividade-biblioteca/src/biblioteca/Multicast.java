package biblioteca;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public final class Multicast {
    static final String GRUPO = "230.0.0.1";
    static final int PORTA = 5002;
    static final int AUTH = 5003;

    public static void servidor() throws Exception {
        ExecutorService autenticacoes = Executors.newFixedThreadPool(8);
        ExecutorService envios = Executors.newFixedThreadPool(4);
        AutenticacaoService auth = new AutenticacaoService();
        try (ServerSocket tcp = new ServerSocket(AUTH); DatagramSocket udp = new DatagramSocket();
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            InetAddress grupo = InetAddress.getByName(GRUPO);
            Thread accept = new Thread(() -> {
                while (!tcp.isClosed()) {
                    try {
                        Socket cliente = tcp.accept();
                        autenticacoes.submit(() -> {
                            try (Socket socket = cliente) {
                                socket.setSoTimeout(5000);
                                DataInputStream in = new DataInputStream(socket.getInputStream());
                                Usuario usuario = new Usuario(in.readUTF(), in.readUTF());
                                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                                out.writeBoolean(auth.autenticar(usuario));
                                out.writeUTF(GRUPO); out.writeInt(PORTA); out.flush();
                            } catch (IOException e) { System.err.println("Autenticação: " + e.getMessage()); }
                        });
                    } catch (IOException e) { if (!tcp.isClosed()) System.err.println(e.getMessage()); }
                }
            }, "autenticacao-tcp");
            accept.start();
            System.out.println("Autenticação TCP 5003. Digite TIPO|mensagem ou sair.");
            String linha;
            while ((linha = console.readLine()) != null && !linha.equalsIgnoreCase("sair")) {
                String[] partes = linha.split("\\|", 2);
                if (partes.length != 2 || !tipoValido(partes[0])) {
                    System.out.println("Use NOTIFICACAO|texto, ALERTA|texto ou ATUALIZACAO|texto"); continue;
                }
                final String tipo = partes[0], mensagem = partes[1];
                envios.submit(() -> {
                    try {
                        byte[] bytes = empacotar(tipo, mensagem, System.currentTimeMillis());
                        if (bytes.length > 60000) throw new IOException("Mensagem grande demais");
                        udp.send(new DatagramPacket(bytes, bytes.length, grupo, PORTA));
                    } catch (IOException e) { System.err.println("Envio: " + e.getMessage()); }
                });
            }
            envios.shutdown(); envios.awaitTermination(5, TimeUnit.SECONDS);
        } finally { autenticacoes.shutdownNow(); envios.shutdownNow(); }
    }

    public static void cliente(String host, String nome, String senha, String interfaceNome) throws Exception {
        String grupo; int porta;
        try (Socket tcp = new Socket(host, AUTH)) {
            tcp.setSoTimeout(5000);
            DataOutputStream out = new DataOutputStream(tcp.getOutputStream());
            out.writeUTF(nome); out.writeUTF(senha); out.flush();
            DataInputStream in = new DataInputStream(tcp.getInputStream());
            if (!in.readBoolean()) { System.out.println("Autenticação recusada"); return; }
            grupo = in.readUTF(); porta = in.readInt();
        }
        NetworkInterface rede = escolherInterface(interfaceNome);
        InetSocketAddress endereco = new InetSocketAddress(InetAddress.getByName(grupo), porta);
        try (MulticastSocket socket = new MulticastSocket(null)) {
            socket.setReuseAddress(true); socket.bind(new InetSocketAddress(porta));
            socket.joinGroup(endereco, rede);
            Thread receptor = new Thread(() -> {
                byte[] buffer = new byte[65507];
                while (!socket.isClosed()) {
                    try {
                        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                        socket.receive(pacote);
                        System.out.println(desempacotar(pacote.getData(), pacote.getLength()));
                    } catch (IOException e) { if (!socket.isClosed()) System.err.println(e.getMessage()); }
                }
            }, "escuta-multicast");
            Thread interacao = new Thread(() -> {
                System.out.println("Grupo " + grupo + " via " + rede.getName() + ". Digite sair.");
                try {
                    BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
                    String linha;
                    while ((linha = console.readLine()) != null && !linha.equalsIgnoreCase("sair")) {
                        System.out.println("Digite sair para deixar o grupo.");
                    }
                } catch (IOException e) { System.err.println(e.getMessage()); }
                finally {
                    try { socket.leaveGroup(endereco, rede); }
                    catch (IOException e) { System.err.println("Saída do grupo: " + e.getMessage()); }
                    socket.close();
                }
            }, "interacao-usuario");
            receptor.start(); interacao.start(); interacao.join(); receptor.join();
        }
    }
    static NetworkInterface escolherInterface(String nome) throws SocketException {
        if (nome != null) {
            NetworkInterface rede = NetworkInterface.getByName(nome);
            if (rede == null || !rede.isUp() || !rede.supportsMulticast())
                throw new SocketException("Interface inválida para multicast: " + nome);
            return rede;
        }
        java.util.Enumeration<NetworkInterface> redes = NetworkInterface.getNetworkInterfaces();
        while (redes.hasMoreElements()) {
            NetworkInterface rede = redes.nextElement();
            if (rede.isUp() && rede.supportsMulticast() && !rede.isLoopback()) return rede;
        }
        throw new SocketException("Nenhuma interface multicast disponível");
    }
    static boolean tipoValido(String tipo) {
        return tipo.equals("NOTIFICACAO") || tipo.equals("ALERTA") || tipo.equals("ATUALIZACAO");
    }
    static byte[] empacotar(String tipo, String mensagem, long timestamp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeUTF(tipo); out.writeUTF(mensagem); out.writeLong(timestamp);
        return bytes.toByteArray();
    }
    static String desempacotar(byte[] bytes, int tamanho) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, tamanho));
        String tipo = in.readUTF(), mensagem = in.readUTF(); long timestamp = in.readLong();
        if (!tipoValido(tipo) || in.available() != 0) throw new IOException("Notificação inválida");
        return "[" + tipo + "] " + mensagem + " (timestamp=" + timestamp + ")";
    }
}
