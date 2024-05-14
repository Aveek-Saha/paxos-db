package Paxos;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface representing the Learner component in the Paxos distributed consensus algorithm. A
 * Learner receives learned values from Acceptors and processes them.
 */
public interface Learner extends Remote {

    /**
     * Notifies the Learner about a learned value.
     *
     * @param value The value learned by the Learner.
     * @return A confirmation message indicating the status of the learning process.
     * @throws RemoteException If there is a communication-related issue during the method
     *                         invocation.
     */
    String learn(String value) throws RemoteException;
}