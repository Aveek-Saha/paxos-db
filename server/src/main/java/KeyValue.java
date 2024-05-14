import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A class for creating and performing operations on a Key Value Store.
 */
class KeyValue {
    public ConcurrentMap<String, String> KVStore;

    /**
     * Create a new KV store in the form of a hashmap.
     */
    public KeyValue() {
        KVStore = new ConcurrentHashMap<>();
    }

    public KeyValue(Map<String, String> loadedKVStore) {
        KVStore = new ConcurrentHashMap<>(loadedKVStore);
    }

    /**
     * Get the value for a given key from a Key Value store if it exists
     *
     * @param key the key to get the corresponding value for in the hashmap
     * @return the value if the key exists or null if it doesn't.
     */
    public String get(String key) {
        return this.KVStore.getOrDefault(key, null);
    }

    /**
     * Put a key value pair into the Key Value store
     *
     * @param key   the key to insert
     * @param value the corresponding value
     * @return true if the operation was successful and false otherwise
     */
    public boolean put(String key, String value) {
        this.KVStore.put(key, value);
        return this.KVStore.containsKey(key);
    }

    /**
     * Delete a Key value pair from the Key Value store
     *
     * @param key the key to delete from the Key value store if it exists
     * @return true if the delete was a success and false otherwise
     */
    public boolean delete(String key) {
        if (this.KVStore.containsKey(key)) {
            this.KVStore.remove(key);
            return true;
        }
        return false;
    }
}
