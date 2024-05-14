import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The interface for a replica server, extending Proposer, Acceptor, and Learner.
 */
public interface ReplicaInterface extends Remote {

    /**
     * Takes in an input request from the client and returns the appropriate response
     *
     * @param requestStr The formatted request as a string
     * @return The response to be sent to the client
     * @throws RemoteException If there is an error during the remote call
     */
    String generateResponse(String requestStr) throws RemoteException;
}
