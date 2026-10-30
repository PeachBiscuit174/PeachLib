package de.peachbiscuit174.peachlib.api.data.structured;

/** Indicates that values or operations do not conform to the table's declared schema. */
public final class SchemaMismatchException extends StructuredDataException {
    /** Creates an exception with a message. */ public SchemaMismatchException(String message) { super(message); }
    /** Creates an exception with a message and root cause. */ public SchemaMismatchException(String message, Throwable cause) { super(message, cause); }
}
