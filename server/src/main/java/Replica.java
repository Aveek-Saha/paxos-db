import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import Paxos.Messages;

/**
 * Represents a replica server implementing the ReplicaInterface.
 */
public class Replica extends UnicastRemoteObject implements ReplicaInterface {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private final ReentrantLock lock;
    private final int serverId;
    private final ExecutorService executorService;
    KeyValue kvs;
    List<ReplicaInterface> peers;
    int instanceNumber;
    double FAILURE_THRESHOLD = 0.25;
    private Map<Integer, Long> highestPromisedProposals; // Iteration Number, Proposal Number
    private Map<Integer, String> acceptedValues; // Iteration Number, Accepted value
    private long proposalNumber;

    /**
     * Initializes the replica server with a key-value store and a lock.
     *
     * @param serverId The ID of the server.
     * @throws RemoteException If there is an issue with remote communication.
     */
    public Replica(int serverId) throws RemoteException {
        super();

        if (PersistState.checkForSavedState() && loadState()) {
            ServerLogger.log("Loading from previous state");
        } else {
            this.kvs = new KeyValue();
            this.highestPromisedProposals = new HashMap<>();
            this.acceptedValues = new HashMap<>();
        }
        this.lock = new ReentrantLock();
        this.peers = new ArrayList<>();
        this.proposalNumber = generateProposalId();
        this.serverId = serverId;
        this.executorService = Executors.newCachedThreadPool();
        this.instanceNumber = counter.get();
    }

    /**
     * Generates a unique proposal ID based on system time and server ID.
     *
     * @return A unique proposal ID.
     */
    private long generateProposalId() {
        long nanoTime = System.nanoTime();
        return nanoTime + this.serverId;
    }

    /**
     * Creates a JSON response object.
     *
     * @param status  The status to be sent in the response.
     * @param message The message to be sent in the response.
     * @param data    The data to be sent in the response.
     * @return A JSON object containing the response.
     */
    public JSONObject jsonResponse(String status, String message, String data) {

        JSONObject response = new JSONObject();
        response.put("status", status);
        response.put("message", message);
        response.put("data", data);

        return response;
    }

    /**
     * Takes in an input request from the client and returns the appropriate response
     *
     * @param requestStr The formatted request as a string
     * @return The response to be sent to the client
     * @throws RemoteException If there is an error during the remote call
     */
    @Override
    public String generateResponse(String requestStr) throws RemoteException {

        String clientName = "unknown";
        try {
            clientName = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            ServerLogger.logError("Error getting client info: " + e.getMessage());
        }

        ServerLogger.log("Received request from " + clientName + ": " + requestStr);

        // Parse request JSON string and create empty JSON response
        JSONObject request;
        JSONObject response;
        try {
            request = new JSONObject(requestStr);
        } catch (JSONException e) {
            ServerLogger.logError("Error parsing JSON: " + e.getMessage());
            response = jsonResponse("400", "Invalid request format", null);
            return response.toString();
        }

        // Process request
        String method = request.getString("method");

        switch (method.toUpperCase()) {
            case "GET":
                String getKey = request.getString("data");
                response = handleGet(getKey);
                break;
            case "PUT":
            case "DEL":
                instanceNumber = serverId + counter.incrementAndGet();
                if (this.propose(requestStr, instanceNumber)) {

                    //ServerLogger.logInfo("Send Learn requests");
                    String resString = this.sendLearnRequests(requestStr);
                    if (resString != null) response = new JSONObject(resString);
                    else response =
                            jsonResponse("400", "Error, operation could not be completed", null);
                } else {
                    response = jsonResponse("400", "Consensus could not be reached", null);
                }
                break;
            default:
                response = jsonResponse("400",
                        "Invalid method. Valid methods are " + "GET, PUT and DEL", null);
                break;
        }

        ServerLogger.log("Sent response to " + clientName + ": " + response);

        return response.toString();
    }

    /**
     * Proposes a value to be accepted by the majority.
     *
     * @param operation      The operation to propose.
     * @param instanceNumber The instance number.
     * @return True if consensus is reached; false otherwise.
     * @throws RemoteException If there is an issue with remote communication.
     */
    @Override
    public boolean propose(String operation, int instanceNumber) throws RemoteException {
        String value = null;
        boolean consensusReached = false;
        int attempt = 0;

        while (!consensusReached) {
            attempt++;
            this.proposalNumber = generateProposalId();
            ServerLogger.logInfo(
                    "Instance: " + instanceNumber + "; New proposal: " + this.proposalNumber);

            JSONObject prepareRequest =
                    Messages.PrepareRequest(this.proposalNumber, instanceNumber);
            //ServerLogger.logInfo("Send prepare requests");
            String[] prepareResponses =
                    sendPrepareRequests(prepareRequest.toString(), instanceNumber);

            if (isPromisedByMajority(prepareResponses)) {
                ServerLogger.logInfo("Instance: " + instanceNumber +
                        "; Received promises from a majority of replicas for " +
                        this.proposalNumber);

                long maxPreviousProposal = getMaxPreviousProposal(prepareResponses);
                ServerLogger.logInfo(
                        "Instance: " + instanceNumber + ": Previous highest proposal" + " " +
                                maxPreviousProposal);
                if (maxPreviousProposal > 0) {
                    value = getValueFromHighestProposal(prepareResponses, maxPreviousProposal);
                }
                if (value == null) {
                    ServerLogger.logInfo(
                            "Instance: " + instanceNumber + "; No previously " + "accepted value");
                    value = operation;
                }

                JSONObject acceptRequest =
                        Messages.AcceptRequest(this.proposalNumber, instanceNumber, value);

                //ServerLogger.logInfo("Send Accept requests");
                String[] acceptResponses =
                        sendAcceptRequests(acceptRequest.toString(), instanceNumber);

                if (isAcceptedByMajority(acceptResponses)) {
                    consensusReached = true;
                    ServerLogger.logInfo("Consensus reached, value accepted by majority");
                } else {
                    ServerLogger.logWarning(
                            "Consensus could not be reached for proposal " + this.proposalNumber +
                                    ", retrying, attempt number: " + attempt);
                }
            }
        }
        return consensusReached;
    }

    /**
     * Sends prepare requests to the replicas.
     *
     * @param requestStr     The request to be sent.
     * @param instanceNumber The instance number.
     * @return An array of prepare responses.
     * @throws RemoteException If there is an issue with remote communication.
     */
    @Override
    public String prepare(String requestStr, int instanceNumber) throws RemoteException {
        lock.lock();
        try {
            ServerLogger.logInfo("Received prepare request from: " + RemoteServer.getClientHost());

            if (emulateFailure(FAILURE_THRESHOLD)) {
                return null;
            }

            JSONObject request = new JSONObject(requestStr);
            long proposalNumber = request.getLong("proposalNumber");
            //int instanceNumber = request.getInt("instanceNumber");
            boolean promised = false;
            Long previouslyPromisedProposal =
                    this.highestPromisedProposals.getOrDefault(instanceNumber, null);
            String previouslyAcceptedValue = this.acceptedValues.get(instanceNumber);

            if (previouslyPromisedProposal == null || proposalNumber > previouslyPromisedProposal) {
                this.highestPromisedProposals.put(instanceNumber, proposalNumber);
                promised = true;
                ServerLogger.logInfo(
                        "Instance: " + instanceNumber + "; Promised proposal: " + proposalNumber);
            }

            return Messages.PrepareResponse(promised, previouslyPromisedProposal,
                    previouslyAcceptedValue).toString();
        } catch (ServerNotActiveException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Accepts a proposal.
     *
     * @param requestStr     The request to be accepted.
     * @param instanceNumber The instance number.
     * @return The accept response as a string.
     * @throws RemoteException If there is an issue with remote communication.
     */
    @Override
    public String accept(String requestStr, int instanceNumber) throws RemoteException {
        lock.lock();
        try {
            ServerLogger.logInfo("Received accept request from: " + RemoteServer.getClientHost());

            if (emulateFailure(FAILURE_THRESHOLD)) {
                return null;
            }

            JSONObject request = new JSONObject(requestStr);
            long proposalNumber = request.getLong("proposalNumber");
            //int instanceNumber = request.getInt("instanceNumber");
            String value = request.getString("value");
            boolean accepted = false;
            Long previouslyPromisedProposal =
                    this.highestPromisedProposals.getOrDefault(instanceNumber, null);
            if (previouslyPromisedProposal == null ||
                    proposalNumber >= previouslyPromisedProposal) {
                this.highestPromisedProposals.put(instanceNumber, proposalNumber);
                this.acceptedValues.put(instanceNumber, value);
                accepted = true;
                ServerLogger.logInfo(
                        "Instance: " + instanceNumber + "; Accepted proposal: " + proposalNumber);
            }

            return Messages.AcceptResponse(accepted).toString();
        } catch (ServerNotActiveException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Simulates communication failures by generating a random number and comparing it with a
     * specified threshold.
     *
     * @param threshold The threshold value for determining failure probability (between 0 and 1).
     * @return `true` if a failure is emulated, `false` otherwise.
     */
    private boolean emulateFailure(double threshold) {
        Random random = new Random();
        // Generate a random number between 0 and 1
        double randomNumber = random.nextDouble();
        if (randomNumber < threshold) {
            ServerLogger.logWarning("Emulating communication failure");
            return true;
        }
        return false;
    }

    /**
     * Learns a value.
     *
     * @param requestStr The request to be learned.
     * @return The learn response as a string.
     * @throws RemoteException If there is an issue with remote communication.
     */
    @Override
    public String learn(String requestStr) throws RemoteException {

        lock.lock();
        try {
            ServerLogger.logInfo("Received learn '" + requestStr + "' request from: " +
                    RemoteServer.getClientHost());

            JSONObject request;
            JSONObject response;
            try {
                request = new JSONObject(requestStr);
            } catch (JSONException e) {
                ServerLogger.logError("Error parsing JSON: " + e.getMessage());
                response = jsonResponse("400", "Invalid request format", null);
                return response.toString();
            }

            String method = request.getString("method");
            // Learner only required for put and delete since they modify the KV store
            switch (method.toUpperCase()) {
                case "PUT":
                    JSONObject data = request.getJSONObject("data");
                    response = handlePut(data);
                    break;
                case "DEL":
                    String delKey = request.getString("data");
                    response = handleDelete(delKey);
                    break;
                default:
                    response = jsonResponse("400", "Invalid learn request", null);
                    break;
            }
            ServerLogger.logInfo("Learnt: " + requestStr);
            if (saveState()) {
                ServerLogger.log("Saved state successfully");
            } else {
                ServerLogger.logError("Failed to save state");
            }
            return response.toString();
        } catch (ServerNotActiveException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * Sends learn requests to all peers in parallel and collects their responses.
     *
     * @param value The value to be learned by the peers.
     * @return A string representation of the response from the peers, or `null` if an error occurs.
     * @throws RemoteException If there is an error during the remote call.
     */
    private String sendLearnRequests(String value) throws RemoteException {
        List<Future<String>> futures = new ArrayList<>();
        for (ReplicaInterface peer : this.peers) {
            futures.add(executorService.submit(() -> peer.learn(value)));
        }

        String responseStr = null;
        for (Future<String> future : futures) {
            try {
                JSONObject response = new JSONObject(future.get());
                responseStr = response.toString();
                if (response.getString("status").equals("400")) {
                    return null;
                }
            } catch (Exception e) {
                ServerLogger.logError("Error sending learn requests: " + e.getMessage());
            }
        }
        return responseStr;
    }

    /**
     * Sends prepare requests to the replicas.
     *
     * @param request        The request to be sent.
     * @param instanceNumber The instance number.
     * @return An array of prepare responses.
     * @throws RemoteException If there is an issue with remote communication.
     */
    private String[] sendPrepareRequests(String request, int instanceNumber)
            throws RemoteException {
        List<Future<String>> futures = new ArrayList<>();
        for (ReplicaInterface peer : this.peers) {
            futures.add(executorService.submit(() -> peer.prepare(request, instanceNumber)));
        }

        String[] responses = new String[this.peers.size()];
        int i = 0;
        for (Future<String> future : futures) {
            try {
                responses[i++] = future.get();
            } catch (Exception e) {
                ServerLogger.logError("Error sending prepare requests: " + e.getMessage());
            }
        }
        return responses;
    }

    /**
     * Sends accept requests to all peers in parallel and collects their responses.
     *
     * @param request        The accept request to be sent to peers.
     * @param instanceNumber The instance number associated with the request.
     * @return An array containing the responses from all peers.
     * @throws RemoteException If there is an error during the remote call.
     */
    private String[] sendAcceptRequests(String request, int instanceNumber) throws RemoteException {
        List<Future<String>> futures = new ArrayList<>();
        for (ReplicaInterface peer : this.peers) {
            futures.add(executorService.submit(() -> peer.accept(request, instanceNumber)));
        }

        String[] responses = new String[this.peers.size()];
        int i = 0;
        for (Future<String> future : futures) {
            try {
                responses[i++] = future.get();
            } catch (Exception e) {
                ServerLogger.logError("Error sending accept requests: " + e.getMessage());
            }
        }
        return responses;
    }

    /**
     * Checks if a majority of peers have promised a proposal.
     *
     * @param responseStrings Array of response strings from peers.
     * @return True if a majority of peers have promised, otherwise false.
     */
    private boolean isPromisedByMajority(String[] responseStrings) {
        int promisedCount = 0;
        for (String responseStr : responseStrings) {
            try {
                JSONObject response = new JSONObject(responseStr);
                if (response.getBoolean("promised")) {
                    promisedCount++;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return promisedCount > (responseStrings.length / 2);
    }

    /**
     * Retrieves the maximum previously promised proposal number from the responses.
     *
     * @param responseStrings Array of response strings from peers.
     * @return The maximum previously promised proposal number.
     */
    private long getMaxPreviousProposal(String[] responseStrings) {
        long maxProposal = 0;
        for (String responseStr : responseStrings) {
            try {
                JSONObject response = new JSONObject(responseStr);
                if (response.has("previouslyPromisedProposal") &&
                        response.getLong("previouslyPromisedProposal") > maxProposal) {
                    maxProposal = response.getLong("previouslyPromisedProposal");
                }
            } catch (Exception e) {
                continue;
            }
        }
        return maxProposal;
    }

    /**
     * Retrieves the value corresponding to the highest previously promised proposal number.
     *
     * @param responseStrings     Array of response strings from peers.
     * @param maxPreviousProposal The maximum previously promised proposal number.
     * @return The value associated with the highest previously promised proposal number.
     */
    private String getValueFromHighestProposal(String[] responseStrings, long maxPreviousProposal) {
        for (String responseStr : responseStrings) {
            try {
                JSONObject response = new JSONObject(responseStr);
                if (response.getLong("previouslyPromisedProposal") == maxPreviousProposal) {
                    return response.getString("previouslyAcceptedValue");
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    /**
     * Checks if a majority of peers have accepted a proposal.
     *
     * @param acceptResponses Array of accept responses from peers.
     * @return True if a majority of peers have accepted, otherwise false.
     */
    private boolean isAcceptedByMajority(String[] acceptResponses) {
        int acceptedCount = 0;
        for (String responseStr : acceptResponses) {
            try {
                JSONObject response = new JSONObject(responseStr);
                if (response.getBoolean("accepted")) {
                    acceptedCount++;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return acceptedCount > (peers.size() / 2);
    }

    /**
     * Saves the current state of the replica, including the key-value store and Paxos-related
     * information.
     *
     * @return True if the state is saved successfully, otherwise false.
     */
    private boolean saveState() {
        ServerLogger.logInfo("Saving current state");
        Map<String, String> kv = new HashMap<>(this.kvs.KVStore);
        try {
            PersistState.saveKvStore(kv);
            PersistState.saveAcceptedValues(this.acceptedValues);
            PersistState.saveAcceptedProposalNumbers(this.highestPromisedProposals);
            return true;
        } catch (IOException e) {
            ServerLogger.logError("Error saving state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads the previous state of the replica, including the key-value store and Paxos-related
     * information.
     *
     * @return True if the state is loaded successfully, otherwise false.
     */
    private boolean loadState() {
        try {
            Map<String, String> kv = PersistState.loadKvStore();
            this.kvs = new KeyValue(kv);
            for (String key : this.kvs.KVStore.keySet()) {
                System.out.println(key + ", " + this.kvs.KVStore.get(key));
            }
            this.acceptedValues = PersistState.loadAcceptedValues();
            this.highestPromisedProposals = PersistState.loadAcceptedProposalNumbers();
            ServerLogger.logInfo("Loaded from previous state");
            return true;
        } catch (IOException | ClassNotFoundException e) {
            ServerLogger.logWarning("Could not load from previous state");
            return false;
        }
    }

    /**
     * Handles getting the corresponding value for a key from the KV store if it exists.
     *
     * @param key the key to be inserted
     * @return the message to return to the client along with any data as a JSON string
     */
    public JSONObject handleGet(String key) {
        String value = this.kvs.get(key);
        String message;
        String status;

        // If the key actually exists return the corresponding value
        if (value != null) {
            ServerLogger.log("Successful GET on key '" + key + "' with value '" + value + "'");
            message = "Got key '" + key + "' with value '" + value + "'";
            status = "200";
        } else {
            ServerLogger.logError("Could not find key '" + key + "'");
            message = "GET FAILED for key '" + key + "'";
            status = "400";
        }
        return jsonResponse(status, message, value);
    }

    /**
     * Handles putting key value pairs into the KV store
     *
     * @param data the key value pair to be inserted, in JSON format
     * @return the message to return to the client along with any data as a JSON string
     */
    public JSONObject handlePut(JSONObject data) {
        lock.lock();
        try {
            String key = data.keys().next();
            String value = data.getString(key);
            String message;
            String status;

            // Return a success if the key was successfully put into the KV store
            if (this.kvs.put(key, value)) {
                ServerLogger.log("Successful PUT on key '" + key + "' with value '" + value + "'");
                message = "Put key '" + key + "' with value '" + value + "'";
                status = "200";
            } else {
                ServerLogger.logError("Could not PUT key '" + key + "'");
                message = "PUT FAILED for key '" + key + "' with value '" + value + "'";
                status = "400";
            }
            return jsonResponse(status, message, null);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Handles deleting a key value pair from the KV store
     *
     * @param key the key to delete from the KV store
     * @return the message to return to the client along with any data as a JSON string
     */
    public JSONObject handleDelete(String key) {
        lock.lock();
        try {
            String message;
            String status;

            // If the key exists and was deleted successfully return a success
            if (this.kvs.delete(key)) {
                ServerLogger.log("Successful DEL on key '" + key + "'");
                message = "Deleted key '" + key + "'";
                status = "200";
            } else {
                ServerLogger.logError("Could not DEL key '" + key + "'");
                message = "DEL FAILED for key '" + key + "'";
                status = "400";
            }
            return jsonResponse(status, message, null);
        } finally {
            lock.unlock();
        }
    }
}
