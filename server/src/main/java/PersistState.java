import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for persisting and loading state data.
 */
public class PersistState {

    private static final String KV_STORE_FILE = "KVStore.dat";
    private static final String ACCEPTED_PROPOSAL_NUMBERS_FILE = "acceptedProposalNumbers.dat";
    private static final String ACCEPTED_VALUES_FILE = "acceptedValues.dat";
    private static final Path SAVE_DIR = FileSystems.getDefault().getPath("data");

    /**
     * Checks if the saved state files exist.
     *
     * @return True if the saved state files exist; false otherwise.
     */
    public static boolean checkForSavedState() {
        return Files.exists(SAVE_DIR.resolve(KV_STORE_FILE)) &&
                Files.exists(SAVE_DIR.resolve(ACCEPTED_PROPOSAL_NUMBERS_FILE)) &&
                Files.exists(SAVE_DIR.resolve(ACCEPTED_VALUES_FILE));
    }

    /**
     * Persists a HashMap to a file.
     *
     * @param map            The HashMap to persist.
     * @param persistenceDir The directory where the file will be saved.
     * @param fileName       The name of the file.
     * @param <K>            The type of the keys in the HashMap.
     * @param <V>            The type of the values in the HashMap.
     * @throws IOException If an I/O error occurs.
     */
    private static <K, V> void persistHashMap(Map<K, V> map, Path persistenceDir, String fileName)
            throws IOException {
        if (!Files.exists(SAVE_DIR)) {
            Files.createDirectories(SAVE_DIR);
        }
        Path tempFilePath = Files.createTempFile(persistenceDir, fileName, null);
        Path persistentFilePath = persistenceDir.resolve(fileName);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tempFilePath)))) {
            oos.writeObject(map);
        }

        try {
            Files.move(tempFilePath, persistentFilePath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback to non-atomic move if the file system does not support atomic moves
            Files.move(tempFilePath, persistentFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Loads the key-value store from a file.
     *
     * @return The loaded key-value store.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    public static Map<String, String> loadKvStore() throws IOException, ClassNotFoundException {
        return loadHashMap(SAVE_DIR, KV_STORE_FILE);
    }

    /**
     * Saves the key-value store to a file.
     *
     * @param kvStore The key-value store to save.
     * @throws IOException If an I/O error occurs.
     */
    public static void saveKvStore(Map<String, String> kvStore) throws IOException {
        persistHashMap(kvStore, SAVE_DIR, KV_STORE_FILE);
    }

    /**
     * Loads the accepted proposal numbers from a file.
     *
     * @return The loaded accepted proposal numbers.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    public static Map<Integer, Long> loadAcceptedProposalNumbers()
            throws IOException, ClassNotFoundException {
        return loadHashMap(SAVE_DIR, ACCEPTED_PROPOSAL_NUMBERS_FILE);
    }

    /**
     * Saves the accepted proposal numbers to a file.
     *
     * @param acceptedProposalNumbers The accepted proposal numbers to save.
     * @throws IOException If an I/O error occurs.
     */
    public static void saveAcceptedProposalNumbers(Map<Integer, Long> acceptedProposalNumbers)
            throws IOException {
        persistHashMap(acceptedProposalNumbers, SAVE_DIR, ACCEPTED_PROPOSAL_NUMBERS_FILE);
    }

    /**
     * Loads the accepted values from a file.
     *
     * @return The loaded accepted values.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    public static Map<Integer, String> loadAcceptedValues()
            throws IOException, ClassNotFoundException {
        return loadHashMap(SAVE_DIR, ACCEPTED_VALUES_FILE);
    }

    /**
     * Saves the accepted values to a file.
     *
     * @param acceptedValues The accepted values to save.
     * @throws IOException If an I/O error occurs.
     */
    public static void saveAcceptedValues(Map<Integer, String> acceptedValues) throws IOException {
        persistHashMap(acceptedValues, SAVE_DIR, ACCEPTED_VALUES_FILE);
    }

    /**
     * Loads a HashMap from a file.
     *
     * @param persistenceDir The directory where the file is located.
     * @param fileName       The name of the file.
     * @param <K>            The type of the keys in the HashMap.
     * @param <V>            The type of the values in the HashMap.
     * @return The loaded HashMap.
     * @throws IOException            If an I/O error occurs.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    private static <K, V> Map<K, V> loadHashMap(Path persistenceDir, String fileName)
            throws IOException, ClassNotFoundException {
        Path persistentFilePath = persistenceDir.resolve(fileName);

        if (!Files.exists(persistentFilePath)) {
            return new HashMap<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(persistentFilePath)))) {
            return (Map<K, V>) ois.readObject();
        }
    }
}
