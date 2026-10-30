package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Public asynchronous API for a structured table. Operations are non-blocking and
 * futures complete on an asynchronous adapter thread; callbacks that access Bukkit
 * state must be marshalled to the server thread, for example with
 * {@code PeachLib.getScheduler().runSync(...)}. Never call {@link CompletableFuture#join()}
 * or {@link CompletableFuture#get()} on the server thread. Implementations and returned
 * snapshots are expected to be thread-safe and immutable where applicable.
 */
public interface StructuredTable {
    /** Returns the immutable table schema. */ @NotNull TableSchematic getSchematic();
    /** Returns the schema's table name. */ @NotNull String getName();
    /** Inserts a row and returns its persisted form, including a generated auto-increment key. */ @NotNull CompletableFuture<Row> insert(@NotNull RowValues values);
    /** Inserts or replaces the row identified by its primary key. */ @NotNull CompletableFuture<Void> set(@NotNull RowValues values);
    /** Finds a row by primary key, or completes with null when absent. */ @NotNull CompletableFuture<@Nullable Row> get(@NotNull Object primaryKey);
    /** Returns the first matching row, or null when no row matches. */ @NotNull CompletableFuture<@Nullable Row> getFirst(@NotNull Filter filter);
    /** Returns an immutable snapshot of all matching rows. */ @NotNull CompletableFuture<List<Row>> query(@NotNull Filter filter);
    /** Returns a snapshot of matching rows subject to ordering and pagination. */ @NotNull CompletableFuture<List<Row>> query(@NotNull Filter filter, @NotNull QueryOptions options);
    /** Updates all rows matching the filter and returns the affected row count. */ @NotNull CompletableFuture<Integer> update(@NotNull Filter filter, @NotNull RowValues values);
    /** Deletes a row by key and reports whether one existed. */ @NotNull CompletableFuture<Boolean> delete(@NotNull Object primaryKey);
    /** Deletes all matching rows and returns the affected row count. */ @NotNull CompletableFuture<Integer> delete(@NotNull Filter filter);
    /** Counts rows matching the filter. */ @NotNull CompletableFuture<Long> count(@NotNull Filter filter);
    /** Tests whether at least one row matches the filter. */ @NotNull CompletableFuture<Boolean> exists(@NotNull Filter filter);
    /** Inserts all values and returns the rows in input order. */ @NotNull CompletableFuture<List<Row>> insertAll(@NotNull Collection<RowValues> values);
    /** Upserts all values. */ @NotNull CompletableFuture<Void> setAll(@NotNull Collection<RowValues> values);
    /** Deletes each supplied key and returns the number of rows deleted. */ @NotNull CompletableFuture<Integer> deleteAll(@NotNull Collection<?> primaryKeys);
    /** Runs work atomically. Application/structured-data failures yield a failed {@link TransactionResult}; infrastructure failures complete exceptionally. */
    @NotNull CompletableFuture<TransactionResult> transaction(@NotNull java.util.function.Consumer<TransactionContext> work);
}
