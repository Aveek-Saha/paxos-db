package Paxos;

import org.json.JSONObject;

/**
 * Utility class for creating messages used in the Paxos distributed consensus algorithm.
 */

public class Messages {

    /**
     * Creates a prepare request message.
     *
     * @param proposalNumber The proposal number associated with the prepare request.
     * @param instanceNumber The instance number associated with the prepare request.
     * @return A JSONObject representing the prepare request message.
     */
    public static JSONObject PrepareRequest(long proposalNumber, int instanceNumber) {
        JSONObject prepareRequest = new JSONObject();
        prepareRequest.put("proposalNumber", proposalNumber);
        prepareRequest.put("instanceNumber", instanceNumber);

        return prepareRequest;
    }

    /**
     * Creates a prepare response message.
     *
     * @param promised                   Flag indicating whether the Acceptor promised to accept the
     *                                   proposal.
     * @param previouslyPromisedProposal The proposal number previously promised by the Acceptor.
     * @param previouslyAcceptedValue    The value previously accepted by the Acceptor.
     * @return A JSONObject representing the prepare response message.
     */
    public static JSONObject PrepareResponse(boolean promised, Long previouslyPromisedProposal,
                                             String previouslyAcceptedValue) {
        JSONObject prepareResponse = new JSONObject();
        prepareResponse.put("promised", promised);
        prepareResponse.put("previouslyPromisedProposal", previouslyPromisedProposal);
        prepareResponse.put("previouslyAcceptedValue", previouslyAcceptedValue);

        return prepareResponse;
    }

    /**
     * Creates an accept request message.
     *
     * @param proposalNumber The proposal number associated with the accept request.
     * @param instanceNumber The instance number associated with the accept request.
     * @param value          The value to be accepted.
     * @return A JSONObject representing the accept request message.
     */
    public static JSONObject AcceptRequest(long proposalNumber, int instanceNumber, String value) {
        JSONObject acceptRequest = new JSONObject();
        acceptRequest.put("proposalNumber", proposalNumber);
        acceptRequest.put("instanceNumber", instanceNumber);
        acceptRequest.put("value", value);

        return acceptRequest;
    }

    /**
     * Creates an accept response message.
     *
     * @param accepted Flag indicating whether the Acceptor accepted the proposal.
     * @return A JSONObject representing the accept response message.
     */
    public static JSONObject AcceptResponse(boolean accepted) {
        JSONObject acceptResponse = new JSONObject();
        acceptResponse.put("accepted", accepted);

        return acceptResponse;
    }
}