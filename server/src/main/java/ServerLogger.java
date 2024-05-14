import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A utility class for printing server logs to stdout and stderr.
 */
class ServerLogger {

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_ORANGE = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";

    /**
     * Print a regular server log message
     *
     * @param message the message to print to the server log
     */
    public static void log(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "inf", ANSI_GREEN);
        System.out.println(logMessage);
    }

    /**
     * Print a server warning log message
     *
     * @param message the message to print to the server log
     */
    public static void logWarning(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "war", ANSI_ORANGE);
        System.out.println(logMessage);
    }

    /**
     * Print a server error log message
     *
     * @param message the message to print to the server log
     */
    public static void logError(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "err", ANSI_RED);
        System.err.println(logMessage);
    }

    /**
     * Print a server info message
     *
     * @param message the message to print to the server log
     */
    public static void logInfo(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "inf", ANSI_BLUE);
        System.out.println(logMessage);
    }

    /**
     * Colours a message using ANSI escape sequences
     *
     * @param message    the message to be coloured
     * @param colourCode the ANSI colour code
     * @return the coloured message using ANSI
     */
    public static String colourText(String message, String colourCode) {
        return colourCode + message + ANSI_RESET;
    }

    /**
     * Makes a message bold using ANSI escape sequences
     *
     * @param message the message to make bold
     * @return the bold message using ANSI
     */
    public static String boldText(String message) {
        return ANSI_BOLD + message + ANSI_RESET;
    }

    /**
     * Constructs a log message
     *
     * @param timestamp  the timestamp of the log
     * @param message    the log message
     * @param level      the severity level of the message
     * @param colourCode the colour code corresponding to the severity
     * @return the constructed log message
     */
    public static String logBuilder(String timestamp, String message, String level,
                                    String colourCode) {
        return colourText(timestamp + " - " + boldText(level.toUpperCase() + ": "), colourCode) +
                message;
    }
}
