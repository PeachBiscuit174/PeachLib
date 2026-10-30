package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/** Immutable query ordering and pagination settings. Instances are safe to share across threads. */
public final class QueryOptions {
    /** Sort direction for the selected order-by column. */
    public enum Direction { ASC, DESC }
    private final @Nullable String orderBy;
    private final @NotNull Direction direction;
    private final int limit;
    private final int offset;
    private QueryOptions(@Nullable String orderBy, @NotNull Direction direction, int limit, int offset) {
        if (limit < 0) throw new IllegalArgumentException("limit must be >= 0");
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
        this.orderBy = orderBy; this.direction = Objects.requireNonNull(direction, "direction"); this.limit = limit; this.offset = offset;
    }
    /** Returns default options with no sorting and no pagination. */ public static @NotNull QueryOptions defaults() { return new QueryOptions(null, Direction.ASC, Integer.MAX_VALUE, 0); }
    /** Starts an options builder. */ public static @NotNull Builder builder() { return new Builder(); }
    /** Returns the order-by column, or null when unspecified. */ public @Nullable String getOrderBy() { return orderBy; }
    /** Returns the selected sort direction. */ public @NotNull Direction getDirection() { return direction; }
    /** Returns maximum number of rows; Integer.MAX_VALUE denotes unbounded default. */ public int getLimit() { return limit; }
    /** Returns the number of matching rows to skip. */ public int getOffset() { return offset; }
    /** Validates that configured columns exist in the given schema. */
    public void validate(@NotNull TableSchematic schema) {
        Objects.requireNonNull(schema, "schema");
        if (orderBy != null && schema.getColumn(orderBy) == null) throw new IllegalArgumentException("Unknown order-by column '" + orderBy + "' in table '" + schema.getName() + "'");
    }
    /** Fluent builder for immutable query options. */
    public static final class Builder {
        private @Nullable String orderBy;
        private @NotNull Direction direction = Direction.ASC;
        private int limit = Integer.MAX_VALUE;
        private int offset;
        private Builder() { }
        /** Sets sort column and direction. */ public @NotNull Builder orderBy(@NotNull String column, @NotNull Direction direction) { this.orderBy = Objects.requireNonNull(column, "column"); if (column.isBlank()) throw new IllegalArgumentException("column must not be blank"); this.direction = Objects.requireNonNull(direction, "direction"); return this; }
        /** Sets maximum number of rows; zero returns no rows. */ public @NotNull Builder limit(int limit) { if (limit < 0) throw new IllegalArgumentException("limit must be >= 0"); this.limit = limit; return this; }
        /** Sets number of rows skipped. */ public @NotNull Builder offset(int offset) { if (offset < 0) throw new IllegalArgumentException("offset must be >= 0"); this.offset = offset; return this; }
        /** Builds immutable options. */ public @NotNull QueryOptions build() { return new QueryOptions(orderBy, direction, limit, offset); }
    }
}
