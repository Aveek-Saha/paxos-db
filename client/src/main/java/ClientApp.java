import org.json.JSONException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The main entrypoint for the client
 */
public class ClientApp {
    /**
     * The starting point for the client.
     *
     * @param args takes two arguments, the hostname/IP of the server, the port number to connect to
     *             the server on.
     */
    public static void main(String[] args) {

        if (args[0].equals("bench")) {
            try {
                if ((args.length - 1) % 2 == 0) {
                    ClientLogger.log("Running benchmark");
                    System.out.println();

                    ConcurrentHashMap<Integer, String> data;
                    int NUM_REQUESTS = 100;
                    int VALUE_LENGTH = 16;

                    ReplicaInterface[] servers = new ReplicaInterface[(args.length - 1) / 2];
                    for (int i = 1; i < args.length; i += 2) {
                        String serverHost = args[i];
                        int serverPort = Integer.parseInt(args[i + 1]);
                        String serverName =
                                InetAddress.getByName(serverHost).getHostAddress() + ":" +
                                        serverPort;
                        Registry registry = LocateRegistry.getRegistry(serverHost, serverPort);
                        ReplicaInterface stub = (ReplicaInterface) registry.lookup(serverName);

                        servers[((i + 1) / 2) - 1] = stub;

                    }

                    data = generateData(NUM_REQUESTS, VALUE_LENGTH);

                    benchmark(servers, "put", data, NUM_REQUESTS);
                    System.out.println();
                    benchmark(servers, "get", data, NUM_REQUESTS);
                    System.out.println();
                    benchmark(servers, "del", data, NUM_REQUESTS);
                    System.out.println();

                } else {
                    ClientLogger.logError(
                            "Incorrect benchmark parameters provided, correct syntax is: " +
                                    "java -jar <path to jar>/client.jar <hostname/IP> <port>");
                    System.exit(1);
                }
            } catch (UnknownHostException | NotBoundException | RemoteException e) {
                throw new RuntimeException(e);
            }
            System.exit(1);
        }

        if (args.length != 2) {
            ClientLogger.logError("Incorrect parameters provided, correct syntax is: " +
                    "java -jar <path to jar>/client.jar <hostname/IP> <port>");
            System.exit(1);
        }

        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try {
            String serverName =
                    InetAddress.getByName(serverHost).getHostAddress() + ":" + serverPort;
            Registry registry = LocateRegistry.getRegistry(serverHost, serverPort);
            ReplicaInterface stub = (ReplicaInterface) registry.lookup(serverName);

            ClientLogger.log("Starting client");
            System.out.println();

            ClientLogger.log("Pre-populating the KV store");
            prePopulateKVStore(stub);
            System.out.println();

            ClientLogger.log("Performing 5 of each type of operation on the KV store");
            performOperations(stub);
            System.out.println();

            ClientLogger.log("Input format is: METHOD KEY [VALUE]");
            ClientLogger.log("Example: put key value");
            ClientLogger.log("Example: get key");
            ClientLogger.log("Example: del key");

            System.out.println();

            try (Scanner scanner = new Scanner(System.in)) {
                // Start an infinite loop to continuously wait for user input and send messages
                while (true) {
                    // Prompt user for method
                    System.out.print("Enter command : ");

                    String input = scanner.nextLine().trim();
                    String request = Client.formatInput(input);
                    if (request == null) {
                        continue;
                    }
                    ClientLogger.log("Request to server: " + request);

                    // Send the request to the server
                    String resString = stub.generateResponse(request);

                    // Get the response from the server
                    if (resString != null) {
                        try {
                            Client.formatResponse(resString);
                        } catch (JSONException e) {
                            ClientLogger.logError("Error parsing JSON: " + e.getMessage());
                        }
                    }

                }
            }
        } catch (Exception e) {
            ClientLogger.logError("Client exception: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Generates a random string of a specified length using the given random number generator.
     *
     * @param random The random number generator.
     * @param length The length of the random string to generate.
     * @return The generated random string.
     */
    private static String generateRandomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) (random.nextInt(26) + 'a'));
        }
        return sb.toString();
    }

    /**
     * Generates a ConcurrentHashMap containing random data.
     *
     * @param NUM_REQUESTS The number of requests to generate.
     * @param VALUE_LENGTH The length of each random value.
     * @return A ConcurrentHashMap containing the generated random data.
     */
    private static ConcurrentHashMap<Integer, String> generateData(int NUM_REQUESTS,
                                                                   int VALUE_LENGTH) {
        Random random = new Random();
        ConcurrentHashMap<Integer, String> data = new ConcurrentHashMap<>();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            String value = generateRandomString(random, VALUE_LENGTH);
            data.put(i, value);
        }

        return data;
    }

    /**
     * Benchmarks the specified method by sending requests to replica servers.
     *
     * @param servers      An array of replica servers to send requests to.
     * @param method       The method to benchmark (get, put, or del).
     * @param data         The data to be used in the benchmark.
     * @param NUM_REQUESTS The number of requests to send.
     */
    private static void benchmark(ReplicaInterface[] servers, String method,
                                  ConcurrentHashMap<Integer, String> data, int NUM_REQUESTS) {
        String[] commands = new String[NUM_REQUESTS];
        ExecutorService executorService = Executors.newFixedThreadPool(NUM_REQUESTS);
        CountDownLatch latch = new CountDownLatch(NUM_REQUESTS);
        int numServers = servers.length;

        for (int key : data.keySet()) {
            switch (method) {
                case "get":
                    commands[key] = Client.formatInput("get " + key);
                    break;
                case "put":
                    commands[key] = Client.formatInput("put " + key + " " + data.get(key));
                    break;
                case "del":
                    commands[key] = Client.formatInput("del " + key);
                    break;
            }
        }

        ClientLogger.log("Benchmark " + method + " operation");

        long startTime = System.nanoTime();

        for (int i = 0; i < commands.length; i++) {
            ReplicaInterface server = servers[i % numServers];
            String command = commands[i];
            executorService.submit(() -> {
                try {
                    //ClientLogger.logInfo("Command: " + command);
                    String response = server.generateResponse(command);
                    Client.formatResponse(response);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        long durationNano = endTime - startTime;

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println(
                "Benchmark completed in " + TimeUnit.NANOSECONDS.toMillis(durationNano) + " ms");
        System.out.println("Average response time " +
                TimeUnit.NANOSECONDS.toMillis(durationNano) / NUM_REQUESTS + " ms/req");
        System.out.println();
    }

    /**
     * Automatically pre-populates the server with some data
     *
     * @param stub an instance of the remote interface
     */
    private static void prePopulateKVStore(ReplicaInterface stub) {
        String[] commands = new String[]{"put hello world", "put create 123", "put dist systems",
                "put name aveek", "put age 25", "put score 100", "put home work", "put lang java"};

        for (String command : commands) {
            try {
                String request = Client.formatInput(command);
                String resString = stub.generateResponse(request);
                Client.formatResponse(resString);
            } catch (RemoteException e) {
                ClientLogger.logError("Client remote exception: " + e.getMessage());
            }
        }
    }

    /**
     * Automatically performs 5 of each type of operation (GET, PUT and DEL)
     *
     * @param stub an instance of the remote interface
     */
    private static void performOperations(ReplicaInterface stub) {
        String[] putCommands =
                new String[]{"put university neu", "put semester spring", "put year 2024",
                        "put course computer science", "put grade A+"};

        for (String command : putCommands) {
            try {
                String request = Client.formatInput(command);
                String resString = stub.generateResponse(request);
                Client.formatResponse(resString);
            } catch (RemoteException e) {
                ClientLogger.logError("Client remote exception: " + e.getMessage());
            }
        }

        String[] getCommands =
                new String[]{"get university", "get semester", "get year", "get course",
                        "get grade"};

        for (String command : getCommands) {
            try {
                String request = Client.formatInput(command);
                String resString = stub.generateResponse(request);
                Client.formatResponse(resString);
            } catch (RemoteException e) {
                ClientLogger.logError("Client remote exception: " + e.getMessage());
            }
        }

        String[] delCommands =
                new String[]{"del university", "del semester", "del year", "del course",
                        "del grade"};

        for (String command : delCommands) {
            try {
                String request = Client.formatInput(command);
                String resString = stub.generateResponse(request);
                Client.formatResponse(resString);
            } catch (RemoteException e) {
                ClientLogger.logError("Client remote exception: " + e.getMessage());
            }
        }
    }
}
