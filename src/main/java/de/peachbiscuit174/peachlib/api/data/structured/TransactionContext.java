package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Synchronous operations available only inside an active transaction callback.
 * Calls use the transaction's connection/session and must not escape the callback
 * or be used concurrently. Throwing from the callback aborts and rolls back the
 * transaction; returned row lists must be immutable snapshots.
 */
public interface TransactionContext {
    /** Inserts and returns the persisted row including any generated key. */ @NotNull Row insert(@NotNull RowValues values);
    /** Inserts or replaces the row identified by its primary key. */ void set(@NotNull RowValues values);
    /** Reads a row by key, returning null when absent. */ @Nullable Row get(@NotNull Object primaryKey);
    /** Reads all rows matching a filter. */ @NotNull List<Row> query(@NotNull Filter filter);
    /** Reads rows using ordering and pagination options. */ @NotNull List<Row> query(@NotNull Filter filter, @NotNull QueryOptions options);
    /** Updates matching rows and returns the affected count. */ int update(@NotNull Filter filter, @NotNull RowValues values);
    /** Deletes by primary key and reports whether a row existed. */ boolean delete(@NotNull Object primaryKey);
    /** Deletes matching rows and returns the affected count. */ int delete(@NotNull Filter filter);
    /** Counts rows matching the filter. */ long count(@NotNull Filter filter);
}
