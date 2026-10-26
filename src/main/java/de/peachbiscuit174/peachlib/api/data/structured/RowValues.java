package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Schema-aware builder for a set of column values intended to be written
 * through {@code insert} or {@code update} operations of the structured data
 * system.
 *
 * <p>
 * The builder accumulates values in insertion order and validates the
 * complete set on demand via {@link #build()}. The following rules apply
 * during validation:
 * </p>
 * <ul>
 *     <li>Every value is converted via its column's
 *         {@link ColumnType#convert(Object)} method. The conversion either
 *         succeeds or throws – no silent loss of type information occurs.</li>
 *     <li>{@code null} is allowed only for columns marked as nullable.</li>
 *     <li>If every column is left out (no value supplied), validation
 *         succeeds and yields an empty row of values.</li>
 *     <li>Primary-key columns are allowed to be omitted only when marked as
 *         {@link ColumnDefinition#isAutoIncrement() auto-increment}.</li>
 * </ul>
 *
 * <p>
 * Why include {@code RowValues} already in step 2 of the design? The
 * validation logic for writes is a mirror of the validation logic used by
 * the read-side {@link Filter} – both delegate to {@link ColumnType#convert}
 * to enforce type fidelity. Extracting the builder here keeps step 3
 * (persistence back-ends) focused on I/O rather than on schema plumbing.
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public final class RowValues {

    private final @NotNull TableSchematic schema;
    private final @NotNull Map<String, Object> values = new LinkedHashMap<>();

    private RowValues(@NotNull TableSchematic schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
    }

    /**
     * @return a new builder bound to {@code schema}.
     */
    public static @NotNull RowValues of(@NotNull TableSchematic schema) {
        return new RowValues(schema);
    }

    /**
     * Stores {@code value} for the supplied column. The value is not
     * validated until {@link #build()} is called.
     *
     * @param column the column name.
     * @param value  the value; may be {@code null} for nullable columns.
     * @return this builder.
     * @throws IllegalArgumentException for an unknown column.
     */
    public @NotNull RowValues set(@NotNull String column, @Nullable Object value) {
        Objects.requireNonNull(column, "column");
        if (schema.getColumn(column) == null) {
            throw new IllegalArgumentException(
                    "RowValues references unknown column '" + column
                            + "' in table '" + schema.getName() + "'");
        }
        values.put(column, value);
        return this;
    }

    /**
     * Validates the accumulated values against {@code schema} and returns
     * an unmodifiable map ready to be handed to a write back-end.
     *
     * @return the validated values keyed by column name.
     */
    public @NotNull Map<String, Object> build() {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            ColumnDefinition def = schema.getColumn(entry.getKey());
            // set() guarantees existence, but defensively recheck.
            if (def == null) {
                throw new IllegalStateException(
                        "Internal error: missing column '" + entry.getKey() + "'");
            }
            Object value = entry.getValue();
            if (value == null) {
                if (!def.isNullable()) {
                    throw new IllegalArgumentException(
                            "RowValues: column '" + entry.getKey()
                                    + "' is not nullable");
                }
                continue;
            }
            try {
                def.getType().convert(value);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "RowValues: value for column '" + entry.getKey()
                                + "' is not convertible to " + def.getType()
                                + " – " + ex.getMessage(), ex);
            }
        }

        // Ensure the primary key is either present or auto-increment.
        try {
            ColumnDefinition pk = schema.getPrimaryKey();
            if (!pk.isAutoIncrement() && !values.containsKey(pk.getName())) {
                throw new IllegalArgumentException(
                        "RowValues: primary key '" + pk.getName()
                                + "' must be supplied (auto-increment is not set)");
            }
        } catch (IllegalStateException ignored) {
            // Schema without a PK – accept any value combination.
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /**
     * Convenience that validates and returns a {@link Row} wrapping the
     * resulting values.
     */
    public @NotNull Row buildRow() {
        return new Row(schema, build());
    }
}
