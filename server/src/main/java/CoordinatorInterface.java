import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * an interface for the coordinator in the two phase commit protocol
 */
public interface CoordinatorInterface extends Remote {

    /**
     * @return
     * @throws RemoteException
     */
    List<ReplicaInterface> getReplicas() throws RemoteException;

    /**
     * @param replicaId
     * @param replica
     * @throws RemoteException
     */
    void addReplica(int replicaId, ReplicaInterface replica) throws RemoteException;

    /**
     * @param replicaId
     * @throws RemoteException
     */
    void removeReplica(int replicaId) throws RemoteException;

    /**
     * @return
     * @throws RemoteException
     */
    int getReplicaCount() throws RemoteException;

    /**
     * @param replicaId
     * @return
     * @throws RemoteException
     */
    ReplicaInterface getReplica(int replicaId) throws RemoteException;
}
