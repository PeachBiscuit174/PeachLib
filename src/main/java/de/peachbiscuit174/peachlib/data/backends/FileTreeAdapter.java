package de.peachbiscuit174.peachlib.data.backends;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.peachbiscuit174.peachlib.api.managers.Credentials;
import de.peachbiscuit174.peachlib.data.StorageAdapter;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Raw I/O implementation for FileTree storage.
 * Saves each document as a separate .json file inside a table-specific directory.
 */
@ApiStatus.Internal
public class FileTreeAdapter implements StorageAdapter {

    private final File dataFolder;
    private String connectionId;
    private File connectionDir;

    // Pattern to prevent Path Traversal exploits (only allows alphanumeric characters, dashes, and underscores)
    private static final Pattern VALID_FILE_NAME = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    public FileTreeAdapter(File dataFolder) {
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
        validateName(tableName);
        File tableDir = new File(connectionDir, tableName);
        if (!tableDir.exists()) {
            tableDir.mkdirs();
        }
    }

    @Override
    public void write(String tableName, String id, String jsonValue, long timestamp) throws Exception {
        File targetFile = getFile(tableName, id);

        if (timestamp <= 0) {
            timestamp = System.currentTimeMillis();
        }

        // Bestehende Datei lesen und vergleichen
        if (targetFile.exists()) {
            String existing = Files.readString(targetFile.toPath());
            JsonObject wrapper = tryParseWrapper(existing);
            if (wrapper != null) {

                long storedTimestamp = wrapper.get("timestamp").getAsLong();
                if (storedTimestamp >= timestamp) {
                    return;
                }
            } else {
                long legacyTimestamp = targetFile.lastModified();
                if (legacyTimestamp >= timestamp) {
                    return;
                }
            }
        }


        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("timestamp", timestamp);
        JsonElement valueElement = JsonParser.parseString(jsonValue);
        if (valueElement == null || valueElement.isJsonNull()) {
            wrapper.add("value", JsonNull.INSTANCE);
        } else {
            wrapper.add("value", valueElement);
        }
        String content = new com.google.gson.GsonBuilder().create().toJson(wrapper);

        Path tempFile = Files.createTempFile(targetFile.getParentFile().toPath(), ".tmp", ".json");
        try {
            Files.writeString(tempFile, content);
            Files.move(tempFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tempFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void delete(String tableName, String id) throws Exception {
        File targetFile = getFile(tableName, id);
        if (targetFile.exists()) {
            Files.delete(targetFile.toPath());
        }
    }

    @Override
    public String read(String tableName, String id) throws Exception {
        File targetFile = getFile(tableName, id);
        if (!targetFile.exists()) {
            return null;
        }
        String raw = Files.readString(targetFile.toPath());

        JsonObject wrapper = tryParseWrapper(raw);
        if (wrapper != null && wrapper.has("value")) {
            JsonElement value = wrapper.get("value");
            if (value.isJsonNull()) {
                return "null";
            }
            return value.toString();
        }
        return raw;
    }

    @Override
    public Set<String> getAllPrimaryKeys(String tableName) throws Exception {
        validateName(tableName);
        Set<String> keys = new HashSet<>();
        File tableDir = new File(connectionDir, tableName);

        File[] files = tableDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    String id = file.getName().substring(0, file.getName().length() - 5);
                    keys.add(id);
                }
            }
        }
        return keys;
    }

    @Override
    public void disconnect() throws Exception {
        // No persistent connection to close for File I/O
    }

    private File getFile(String tableName, String id) {
        validateName(tableName);
        validateName(id);
        File tableDir = new File(connectionDir, tableName);
        return new File(tableDir, id + ".json");
    }


    private JsonObject tryParseWrapper(String raw) {
        try {
            JsonElement root = JsonParser.parseString(raw);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject obj = root.getAsJsonObject();
            if (!obj.has("timestamp") || !obj.has("value")) {
                return null;
            }
            if (!obj.get("timestamp").isJsonPrimitive() ||
                !obj.get("timestamp").getAsJsonPrimitive().isNumber()) {
                return null;
            }
            return obj;
        } catch (Exception e) {
            return null;
        }
    }
}