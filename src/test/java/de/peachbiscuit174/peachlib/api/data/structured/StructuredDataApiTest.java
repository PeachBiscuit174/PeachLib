package de.peachbiscuit174.peachlib.api.data.structured;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class StructuredDataApiTest {
    private static final TableSchematic SCHEMA = TableSchematic.builder("users")
            .column("id", ColumnType.LONG).primaryKey().autoIncrement()
            .column("name", ColumnType.VARCHAR(64)).nullable()
            .build();

    @Test
    void queryOptionsValidateColumnAndNonNegativePagination() {
        QueryOptions options = QueryOptions.builder().orderBy("id", QueryOptions.Direction.DESC)
                .limit(5).offset(3).build();
        options.validate(SCHEMA);
        assertEquals(5, options.getLimit());
        assertEquals(3, options.getOffset());
        assertThrows(IllegalArgumentException.class, () -> QueryOptions.builder().limit(-1));
        assertThrows(IllegalArgumentException.class, () -> QueryOptions.builder().offset(-1));
        assertThrows(IllegalArgumentException.class,
                () -> QueryOptions.builder().orderBy("missing", QueryOptions.Direction.ASC).build().validate(SCHEMA));
    }

    @Test
    void transactionResultCarriesFailureAndCanRethrow() {
        TransactionResult success = TransactionResult.success(4);
        assertTrue(success.success());
        assertEquals(4, success.getAffectedRows());
        assertNull(success.getError());
        assertSame(success, success.orThrow());

        StructuredDataException error = new SchemaMismatchException("invalid schema");
        TransactionResult failed = TransactionResult.failure(error, 0);
        assertFalse(failed.success());
        assertSame(error, failed.getError());
        assertThrows(SchemaMismatchException.class, failed::orThrow);
    }

    @Test
    void abstractTableValidatesRowFilterAndOptionsBeforeAdapter() {
        TestTable table = new TestTable();
        assertThrows(IllegalArgumentException.class, () -> table.query(Filter.eq("unknown", 1)));
        assertThrows(IllegalArgumentException.class, () -> table.query(Filter.all(),
                QueryOptions.builder().orderBy("unknown", QueryOptions.Direction.ASC).build()));
        RowValues invalid = RowValues.of(SCHEMA).set("name", 42);
        assertThrows(IllegalArgumentException.class, () -> table.insert(invalid));
        assertEquals(0, table.adapterCalls);
        assertDoesNotThrow(() -> table.query(Filter.eq("id", 2L)).join());
        assertEquals(1, table.adapterCalls);
    }

    @Test
    void longFilterRejectsFractionButAcceptsIntegralDouble() {
        assertThrows(IllegalArgumentException.class, () -> Filter.eq("id", 2.5).validate(SCHEMA));
        assertDoesNotThrow(() -> Filter.eq("id", 2.0).validate(SCHEMA));
    }

    private static final class TestTable extends AbstractStructuredTable {
        private int adapterCalls;
        private TestTable() { super(SCHEMA); }
        private <T> CompletableFuture<T> called(T result) { adapterCalls++; return CompletableFuture.completedFuture(result); }
        @Override protected CompletableFuture<Row> doInsert(RowValues v) { return called(v.buildRow()); }
        @Override protected CompletableFuture<Void> doSet(RowValues v) { return called(null); }
        @Override protected CompletableFuture<Row> doGet(Object key) { return called(null); }
        @Override protected CompletableFuture<Row> doGetFirst(Filter f) { return called(null); }
        @Override protected CompletableFuture<List<Row>> doQuery(Filter f, QueryOptions o) { return called(List.of()); }
        @Override protected CompletableFuture<Integer> doUpdate(Filter f, RowValues v) { return called(0); }
        @Override protected CompletableFuture<Boolean> doDelete(Object key) { return called(false); }
        @Override protected CompletableFuture<Integer> doDelete(Filter f) { return called(0); }
        @Override protected CompletableFuture<Long> doCount(Filter f) { return called(0L); }
        @Override protected CompletableFuture<Boolean> doExists(Filter f) { return called(false); }
        @Override protected CompletableFuture<TransactionResult> doTransaction(Consumer<TransactionContext> work) {
            work.accept(new TransactionContext() {
                public Row insert(RowValues v) { return v.buildRow(); }
                public void set(RowValues v) { v.build(); }
                public Row get(Object key) { return null; }
                public List<Row> query(Filter f) { return List.of(); }
                public List<Row> query(Filter f, QueryOptions o) { return List.of(); }
                public int update(Filter f, RowValues v) { return 0; }
                public boolean delete(Object key) { return false; }
                public int delete(Filter f) { return 0; }
                public long count(Filter f) { return 0; }
            });
            return CompletableFuture.completedFuture(TransactionResult.success(0));
        }
    }
}
