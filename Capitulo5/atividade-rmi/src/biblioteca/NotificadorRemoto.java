package biblioteca;
import java.rmi.*;
public interface NotificadorRemoto extends Remote {
    void receber(String notificacaoJson) throws RemoteException;
}
