package de.peachbiscuit174.peachlib.api.managers;

import de.peachbiscuit174.peachlib.api.files.FileCompressionAPI;
import de.peachbiscuit174.peachlib.api.files.FileUtilAPI;

/**
 * Manager for file-related operations and utilities.
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public class FileManager {

    private final FileCompressionAPI fileCompressionAPI = new FileCompressionAPI();
    private final FileUtilAPI fileUtilAPI = new FileUtilAPI();

    /**
     * Gets the API responsible for asynchronous, secure file compression and decompression.
     *
     * @return The {@link FileCompressionAPI} instance.
     */
    public FileCompressionAPI getFileCompressionAPI() {
        return fileCompressionAPI;
    }

    /**
     * Gets the API responsible for asynchronous directory and file manipulations.
     *
     * @return The {@link FileUtilAPI} instance.
     */
    public FileUtilAPI getFileUtilAPI() {
        return fileUtilAPI;
    }
}