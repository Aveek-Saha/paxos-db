package Paxos;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface representing the Acceptor in the Paxos distributed consensus algorithm. An Acceptor
 * receives prepare and accept messages from the Proposers and responds accordingly.
 */
public interface Acceptor extends Remote {

    /**
     * Responds to a prepare message from a Proposer.
     *
     * @param request        The prepare request message sent by the Proposer.
     * @param instanceNumber The instance number associated with the prepare request.
     * @return The response message from the Acceptor.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    String prepare(String request, int instanceNumber) throws RemoteException;

    /**
     * Responds to an accept message from a Proposer.
     *
     * @param request        The accept request message sent by the Proposer.
     * @param instanceNumber The instance number associated with the accept request.
     * @return The response message from the Acceptor.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    String accept(String request, int instanceNumber) throws RemoteException;
}
