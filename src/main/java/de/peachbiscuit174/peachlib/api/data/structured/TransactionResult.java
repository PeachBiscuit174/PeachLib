package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/** Immutable outcome of a transaction. A failed transaction is a normal result, whereas infrastructure failures may complete the future exceptionally. */
public final class TransactionResult {
    private final boolean success;
    private final @Nullable StructuredDataException error;
    private final int affectedRows;
    private TransactionResult(boolean success, @Nullable StructuredDataException error, int affectedRows) {
        if (affectedRows < 0) throw new IllegalArgumentException("affectedRows must be >= 0");
        if (success == (error != null)) throw new IllegalArgumentException("Successful result must have no error; failed result must have an error");
        this.success = success; this.error = error; this.affectedRows = affectedRows;
    }
    /** Creates a successful result with its aggregate affected-row count. */ public static @NotNull TransactionResult success(int affectedRows) { return new TransactionResult(true, null, affectedRows); }
    /** Creates a failed result with its transaction error and affected-row count (normally zero after rollback). */ public static @NotNull TransactionResult failure(@NotNull StructuredDataException error, int affectedRows) { return new TransactionResult(false, Objects.requireNonNull(error, "error"), affectedRows); }
    /** Reports whether the transaction committed successfully. */ public boolean success() { return success; }
    /** Returns the structured failure, or null on success. */ public @Nullable StructuredDataException getError() { return error; }
    /** Returns affected rows reported by the adapter. */ public int getAffectedRows() { return affectedRows; }
    /** Returns this result on success or throws the stored error on failure. */ public @NotNull TransactionResult orThrow() { if (error != null) throw error; return this; }
}
