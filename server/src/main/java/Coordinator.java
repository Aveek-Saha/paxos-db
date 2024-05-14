import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class representing a coordinator for the replica servers in the Paxos consensus algorithm.
 */
public class Coordinator implements CoordinatorInterface {
    private final Map<Integer, ReplicaInterface> replicas;

    /**
     * A constructor for the coordinator that initializes the list of replicas
     */
    public Coordinator() {
        this.replicas = new HashMap<>();
    }


    /**
     * Adds a replica to the coordinator.
     *
     * @param replicaId The ID of the replica to be added.
     * @param replica   The ReplicaInterface representing the replica to be added.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    @Override
    public void addReplica(int replicaId, ReplicaInterface replica) throws RemoteException {
        if (!this.replicas.containsKey(replicaId)) {
            this.replicas.put(replicaId, replica);
            ServerLogger.log("Added replica " + this.replicas.size());
        }
    }

    /**
     * Removes a replica from the coordinator.
     *
     * @param replicaId The ID of the replica to be removed.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    @Override
    public void removeReplica(int replicaId) throws RemoteException {
        this.replicas.remove(replicaId);
        ServerLogger.log("Removed replica, current count is " + this.replicas.size());
    }

    /**
     * Retrieves the total count of replicas managed by the coordinator.
     *
     * @return The total count of replicas.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    @Override
    public int getReplicaCount() throws RemoteException {
        return this.replicas.size();
    }

    /**
     * Retrieves a list of all replicas managed by the coordinator.
     *
     * @return A list of ReplicaInterface representing all replicas.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    @Override
    public List<ReplicaInterface> getReplicas() throws RemoteException {
        return new ArrayList<>(this.replicas.values());
    }

    /**
     * Retrieves the replica associated with the specified replica ID.
     *
     * @param replicaId The ID of the replica to retrieve.
     * @return The ReplicaInterface representing the specified replica.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    @Override
    public ReplicaInterface getReplica(int replicaId) throws RemoteException {
        return this.replicas.get(replicaId);
    }
}
