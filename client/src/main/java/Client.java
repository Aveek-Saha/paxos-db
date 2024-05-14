import org.json.JSONObject;

/**
 * A class with some helper methods for processing user inputs and requests
 */
class Client {
    /**
     * Converts input from the user into a JSON format
     *
     * @param input the user input from the terminal
     * @return a JSON object format of the input data
     */
    public static String formatInput(String input) {

        String[] inputs = input.split(" ", 3);

        // Checks if the input is in the correct format
        if (inputs.length != 2 && inputs.length != 3) {
            ClientLogger.logError("Incorrect command syntax");
            return null;
        }

        String method = inputs[0];

        // Validate method
        if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("PUT") &&
                !method.equalsIgnoreCase("DEL")) {
            ClientLogger.logError("Invalid method. Valid methods are GET, PUT, or DELETE.");
            return null;
        }

        // Prepare request based on method
        JSONObject request = new JSONObject();
        if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DEL")) {
            if (inputs.length != 2) {
                ClientLogger.logError("Incorrect syntax for " + method.toUpperCase());
                return null;
            }
            String key = inputs[1];
            request.put("method", method);
            request.put("data", key);

        } else {
            if (inputs.length != 3) {
                ClientLogger.logError("Incorrect syntax for " + method.toUpperCase());
                return null;
            }
            String key = inputs[1];
            String value = inputs[2];

            request.put("method", method);
            request.put("data", new JSONObject().put(key, value));
        }

        return request.toString();
    }

    /**
     * Handles the output from a server, matching the checksum and logging the output
     *
     * @param resString The response from the server in String form
     */
    public static void formatResponse(String resString) {

        JSONObject response = new JSONObject(resString);
        String status = response.getString("status");
        String message = response.getString("message");

        if (status.equalsIgnoreCase("400")) {
            ClientLogger.logError(message);
        } else {
            ClientLogger.log(message);
        }
    }
}
