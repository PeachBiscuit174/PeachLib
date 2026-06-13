package de.peachbiscuit174.peachlib.data.backends;

import de.peachbiscuit174.peachlib.api.managers.Credentials;
import de.peachbiscuit174.peachlib.data.StorageAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * YAML implementation for storage.
 * Saves the entire table as a single .yml file where IDs are configuration keys.
 * * NOTE: This adapter executes heavy I/O operations (parsing the entire file) on every read/write.
 * It is meant for small configuration datasets, not for high-frequency database operations.
 */
@ApiStatus.Internal
public class YAMLAdapter implements StorageAdapter {

    private final File dataFolder;
    private String connectionId;
    private File connectionDir;

    private static final Pattern VALID_FILE_NAME = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    public YAMLAdapter(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public void connect(Credentials credentials) throws Exception {
        this.connectionId = credentials.database();
        this.connectionDir = new File(dataFolder, "Data/" + connectionId);

        if (!connectionDir.exists()) {
            connectionDir.mkdirs();
        }
    }

    private void validateName(String name) {
        if (name == null || !VALID_FILE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid identifier to prevent path traversal: " + name);
        }
    }

    @Override
    public void createTableIfNotExists(String tableName) throws Exception {
        File tableFile = getTableFile(tableName);
        if (!tableFile.exists()) {
            tableFile.createNewFile();
        }
    }

    @Override
    public void write(String tableName, String id, String jsonValue, long timestamp) throws Exception {
        validateName(id);
        File tableFile = getTableFile(tableName);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(tableFile);

        config.set(id + ".value", jsonValue);
        config.set(id + ".timestamp", timestamp);

        config.save(tableFile);
    }

    @Override
    public void delete(String tableName, String id) throws Exception {
        validateName(id);
        File tableFile = getTableFile(tableName);
        if (!tableFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(tableFile);
        config.set(id, null);
        config.save(tableFile);
    }

    @Override
    public String read(String tableName, String id) throws Exception {
        validateName(id);
        File tableFile = getTableFile(tableName);
        if (!tableFile.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(tableFile);
        return config.getString(id + ".value");
    }

    @Override
    public Set<String> getAllPrimaryKeys(String tableName) throws Exception {
        File tableFile = getTableFile(tableName);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(tableFile);

        return config.getKeys(false);
    }

    @Override
    public void disconnect() throws Exception {
        // No persistent connection to close for YAML
    }

    private File getTableFile(String tableName) {
        validateName(tableName);
        return new File(connectionDir, tableName + ".yml");
    }
}