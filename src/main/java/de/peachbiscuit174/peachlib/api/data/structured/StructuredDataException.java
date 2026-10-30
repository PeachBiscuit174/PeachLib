package de.peachbiscuit174.peachlib.api.data.structured;

/** Base unchecked exception for structured-data failures. */
public class StructuredDataException extends RuntimeException {
    /** Creates an exception with a message. */ public StructuredDataException(String message) { super(message); }
    /** Creates an exception with a message and root cause. */ public StructuredDataException(String message, Throwable cause) { super(message, cause); }
}
