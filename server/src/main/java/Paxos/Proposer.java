package Paxos;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface representing the Proposer in the Paxos distributed consensus algorithm. A Proposer
 * initiates the Paxos protocol by making proposals to Acceptors.
 */
public interface Proposer extends Remote {

    /**
     * Initiates a proposal for a Paxos instance.
     *
     * @param operation      The operation to be proposed.
     * @param instanceNumber The instance number associated with the proposal.
     * @return True if the proposal was successfully initiated; false otherwise.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    boolean propose(String operation, int instanceNumber) throws RemoteException;
}
