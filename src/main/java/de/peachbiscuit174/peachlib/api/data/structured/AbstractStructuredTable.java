package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Asynchronous contract for a table described by a {@link TableSchematic}.
 * Implementations validate all caller arguments before delegating to the storage
 * adapter. Adapter hooks are protected so persistence mechanics remain outside
 * the public API. Returned rows and collections must be immutable snapshots;
 * implementations must safely publish results across threads.
 *
 * @since 1.0.0
 */
public abstract class AbstractStructuredTable implements StructuredTable {
    private final TableSchematic schematic;

    /** Creates a table API bound to the immutable schema. */
    protected AbstractStructuredTable(@NotNull TableSchematic schematic) {
        this.schematic = Objects.requireNonNull(schematic, "schematic");
    }

    @Override
    public final @NotNull TableSchematic getSchematic() { return schematic; }

    @Override
    public final @NotNull String getName() { return schematic.getName(); }

    /** Validates a row-values object against this table and returns its validated immutable map. */
    protected final @NotNull java.util.Map<String, Object> validate(@NotNull RowValues values) {
        Objects.requireNonNull(values, "values");
        if (!schematic.equals(values.buildRow().getSchema())) {
            throw new SchemaMismatchException("RowValues schema does not match table '" + getName() + "'");
        }
        return values.build();
    }

    /** Validates that a filter is non-null and references valid schema columns. */
    protected final void validate(@NotNull Filter filter) {
        Objects.requireNonNull(filter, "filter").validate(schematic);
    }

    /** Validates query options against this table's schema. */
    protected final void validate(@NotNull QueryOptions options) {
        Objects.requireNonNull(options, "options").validate(schematic);
    }

    @Override public final @NotNull CompletableFuture<Row> insert(@NotNull RowValues values) {
        validate(values); return doInsert(values);
    }
    @Override public final @NotNull CompletableFuture<Void> set(@NotNull RowValues values) {
        validate(values); return doSet(values);
    }
    @Override public final @NotNull CompletableFuture<@Nullable Row> get(@NotNull Object primaryKey) {
        Objects.requireNonNull(primaryKey, "primaryKey"); validatePrimaryKey(primaryKey); return doGet(primaryKey);
    }
    @Override public final @NotNull CompletableFuture<@Nullable Row> getFirst(@NotNull Filter filter) {
        validate(filter); return doGetFirst(filter);
    }
    @Override public final @NotNull CompletableFuture<List<Row>> query(@NotNull Filter filter) {
        validate(filter); return doQuery(filter, QueryOptions.defaults());
    }
    @Override public final @NotNull CompletableFuture<List<Row>> query(@NotNull Filter filter, @NotNull QueryOptions options) {
        validate(filter); validate(options); return doQuery(filter, options);
    }
    @Override public final @NotNull CompletableFuture<Integer> update(@NotNull Filter filter, @NotNull RowValues values) {
        validate(filter); validate(values); return doUpdate(filter, values);
    }
    @Override public final @NotNull CompletableFuture<Boolean> delete(@NotNull Object primaryKey) {
        Objects.requireNonNull(primaryKey, "primaryKey"); validatePrimaryKey(primaryKey); return doDelete(primaryKey);
    }
    @Override public final @NotNull CompletableFuture<Integer> delete(@NotNull Filter filter) {
        validate(filter); return doDelete(filter);
    }
    @Override public final @NotNull CompletableFuture<Long> count(@NotNull Filter filter) {
        validate(filter); return doCount(filter);
    }
    @Override public final @NotNull CompletableFuture<Boolean> exists(@NotNull Filter filter) {
        validate(filter); return doExists(filter);
    }

    @Override public final @NotNull CompletableFuture<List<Row>> insertAll(@NotNull Collection<RowValues> values) {
        List<RowValues> copy = List.copyOf(Objects.requireNonNull(values, "values"));
        copy.forEach(this::validate);
        return sequence(copy.stream().map(this::doInsert).toList());
    }
    @Override public final @NotNull CompletableFuture<Void> setAll(@NotNull Collection<RowValues> values) {
        List<RowValues> copy = List.copyOf(Objects.requireNonNull(values, "values"));
        copy.forEach(this::validate);
        return CompletableFuture.allOf(copy.stream().map(this::doSet).toArray(CompletableFuture[]::new));
    }
    @Override public final @NotNull CompletableFuture<Integer> deleteAll(@NotNull Collection<?> primaryKeys) {
        List<?> copy = List.copyOf(Objects.requireNonNull(primaryKeys, "primaryKeys"));
        copy.forEach(key -> { validatePrimaryKey(Objects.requireNonNull(key, "primaryKey")); });
        return sequenceBooleans(copy.stream().map(this::doDelete).toList());
    }

    @Override
    public final @NotNull CompletableFuture<TransactionResult> transaction(@NotNull Consumer<TransactionContext> work) {
        Objects.requireNonNull(work, "work");
        CompletableFuture<TransactionResult> future;
        try {
            future = Objects.requireNonNull(doTransaction(context -> {
                try {
                    work.accept(new ValidatingContext(context));
                } catch (StructuredDataException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw new StructuredDataException("Transaction callback failed", e);
                }
            }), "transaction future");
        } catch (StructuredDataException e) {
            return CompletableFuture.completedFuture(TransactionResult.failure(e, 0));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        return future.handle((result, error) -> {
            if (error == null) return Objects.requireNonNull(result, "transaction result");
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (cause instanceof StructuredDataException dataException) return TransactionResult.failure(dataException, 0);
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new CompletionException(cause);
        });
    }

    private void validatePrimaryKey(Object key) {
        ColumnDefinition pk;
        try { pk = schematic.getPrimaryKey(); }
        catch (IllegalStateException e) { throw new SchemaMismatchException("Table '" + getName() + "' must define exactly one primary key", e); }
        try { pk.getType().convert(key); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid primary key for table '" + getName() + "'", e); }
    }

    private final class ValidatingContext implements TransactionContext {
        private final TransactionContext delegate;
        private ValidatingContext(TransactionContext delegate) { this.delegate = Objects.requireNonNull(delegate, "context"); }
        public Row insert(RowValues v) { validate(v); return delegate.insert(v); }
        public void set(RowValues v) { validate(v); delegate.set(v); }
        public @Nullable Row get(Object key) { Objects.requireNonNull(key, "primaryKey"); validatePrimaryKey(key); return delegate.get(key); }
        public List<Row> query(Filter f) { validate(f); return List.copyOf(delegate.query(f)); }
        public List<Row> query(Filter f, QueryOptions o) { validate(f); validate(o); return List.copyOf(delegate.query(f, o)); }
        public int update(Filter f, RowValues v) { validate(f); validate(v); return delegate.update(f, v); }
        public boolean delete(Object key) { Objects.requireNonNull(key, "primaryKey"); validatePrimaryKey(key); return delegate.delete(key); }
        public int delete(Filter f) { validate(f); return delegate.delete(f); }
        public long count(Filter f) { validate(f); return delegate.count(f); }
    }

    private static @NotNull CompletableFuture<List<Row>> sequence(List<CompletableFuture<Row>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }
    private static @NotNull CompletableFuture<Integer> sequenceBooleans(List<CompletableFuture<Boolean>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> (int) futures.stream().filter(f -> f.join()).count());
    }

    protected abstract @NotNull CompletableFuture<Row> doInsert(@NotNull RowValues values);
    protected abstract @NotNull CompletableFuture<Void> doSet(@NotNull RowValues values);
    protected abstract @NotNull CompletableFuture<@Nullable Row> doGet(@NotNull Object primaryKey);
    protected abstract @NotNull CompletableFuture<@Nullable Row> doGetFirst(@NotNull Filter filter);
    protected abstract @NotNull CompletableFuture<List<Row>> doQuery(@NotNull Filter filter, @NotNull QueryOptions options);
    protected abstract @NotNull CompletableFuture<Integer> doUpdate(@NotNull Filter filter, @NotNull RowValues values);
    protected abstract @NotNull CompletableFuture<Boolean> doDelete(@NotNull Object primaryKey);
    protected abstract @NotNull CompletableFuture<Integer> doDelete(@NotNull Filter filter);
    protected abstract @NotNull CompletableFuture<Long> doCount(@NotNull Filter filter);
    protected abstract @NotNull CompletableFuture<Boolean> doExists(@NotNull Filter filter);
    /** The adapter must execute {@code work} atomically on its transaction thread and roll back when it throws. */
    protected abstract @NotNull CompletableFuture<TransactionResult> doTransaction(@NotNull Consumer<TransactionContext> work);
}
