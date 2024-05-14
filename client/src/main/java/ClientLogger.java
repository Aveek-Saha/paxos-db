import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A utility class for printing client logs to stdout and stderr.
 */
class ClientLogger {

    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_ORANGE = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";

    /**
     * Print a regular client log message
     *
     * @param message the message to print to the client log
     */
    public static void log(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "inf", ANSI_GREEN);
        System.out.println(logMessage);
    }

    /**
     * Print a client warning log message
     *
     * @param message the message to print to the client log
     */
    public static void logWarning(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "war", ANSI_ORANGE);
        System.out.println(logMessage);
    }

    /**
     * Print a client error log message
     *
     * @param message the message to print to the client log
     */
    public static void logError(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "err", ANSI_RED);
        System.err.println(logMessage);
    }

    public static void logInfo(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = logBuilder(timestamp, message, "inf", ANSI_BLUE);
        System.out.println(logMessage);
    }

    public static String colourText(String message, String colourCode) {
        return colourCode + message + ANSI_RESET;
    }

    public static String boldText(String message) {
        return ANSI_BOLD + message + ANSI_RESET;
    }

    public static String logBuilder(String timestamp, String message, String level,
                                    String colourCode) {
        return colourText(timestamp + " - " + boldText(level.toUpperCase() + ": "), colourCode) +
                message;
    }
}
