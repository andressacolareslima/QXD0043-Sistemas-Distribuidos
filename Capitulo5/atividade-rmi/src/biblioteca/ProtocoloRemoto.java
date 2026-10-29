package biblioteca;
import java.rmi.*;
public interface ProtocoloRemoto extends Remote {
    String doOperation(String requestJson) throws RemoteException;
}
