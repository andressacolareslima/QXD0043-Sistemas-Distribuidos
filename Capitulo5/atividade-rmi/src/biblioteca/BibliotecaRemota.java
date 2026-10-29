package biblioteca;
import java.rmi.*;
public interface BibliotecaRemota extends Remote {
    // O retorno é uma referência remota; o callback também passa por referência.
    ProtocoloRemoto autenticar(String credenciaisJson, NotificadorRemoto callback) throws RemoteException;
}
