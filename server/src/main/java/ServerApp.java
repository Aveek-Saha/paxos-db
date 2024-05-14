import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * The main entrypoint for the server
 */
public class ServerApp {

    /**
     * The starting point for the server.
     *
     * @param args takes one argument, the registry port to start the server with
     */
    public static void main(String[] args) {

        if (args.length != 4 && args.length != 2) {
            ServerLogger.logError("Incorrect parameters provided, correct syntax is: " +
                    "java -jar <path to jar>/server.jar " +
                    "<coordinator hostname> <coordinator port> <port> <num replicas>\n OR \n" +
                    "java -jar <path to jar>/server.jar c <port>");
            System.exit(1);
        }

        if (args[0].equals("c")) {
            try {
                int port = Integer.parseInt(args[1]);
                Registry registry = LocateRegistry.createRegistry(port);

                Coordinator coordinator = new Coordinator();
                CoordinatorInterface stub =
                        (CoordinatorInterface) UnicastRemoteObject.exportObject(coordinator, 0);
                registry.bind("RemoteCoordinator", stub);

            } catch (Exception e) {
                ServerLogger.logError("Coordinator exception: " + e);
            }
        } else {
            try {
                String coordinatorHostname = args[0];
                int coordinatorPort = Integer.parseInt(args[1]);
                int serverPort = Integer.parseInt(args[2]);
                int numberOfReplicas = Integer.parseInt(args[3]);
                Registry registry =
                        LocateRegistry.getRegistry(coordinatorHostname, coordinatorPort);
                CoordinatorInterface coordinator =
                        (CoordinatorInterface) registry.lookup("RemoteCoordinator");

                String serverName = InetAddress.getLocalHost().getHostAddress() + ":" + serverPort;
                int serverId = serverName.hashCode();
                Replica replica = new Replica(serverId);
                coordinator.addReplica(serverId, replica);
                ServerLogger.log("Connected to coordinator");
                checkReplicaCount(coordinator, numberOfReplicas, 10);

                replica.peers = coordinator.getReplicas();

                registry = LocateRegistry.createRegistry(serverPort);
                registry.bind(serverName, replica);

                ServerLogger.log("Server ready: " + serverName);

            } catch (Exception e) {
                ServerLogger.logError("Server exception: " + e);
                e.printStackTrace();
            }

        }
    }

    /**
     * Checks the number of replicas registered with the coordinator and waits until the expected
     * number is reached.
     *
     * @param coordinator      The coordinator interface to query.
     * @param numberOfReplicas The expected number of replicas.
     * @param maxAttempts      The maximum number of attempts to wait for the replicas.
     */
    public static void checkReplicaCount(CoordinatorInterface coordinator, int numberOfReplicas,
                                         int maxAttempts) {
        try {
            int replicaCount;
            int attemptNumber = 0;

            while (attemptNumber < maxAttempts) {
                ServerLogger.logInfo("Waiting for all replicas to start, attempt " + attemptNumber);
                replicaCount = coordinator.getReplicaCount();
                attemptNumber++;
                if (replicaCount == numberOfReplicas) {
                    break;
                }
                // Exponential backoff: wait for 2^attemptNumber seconds before the next attempt
                Thread.sleep(1000L * (1L << attemptNumber));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}