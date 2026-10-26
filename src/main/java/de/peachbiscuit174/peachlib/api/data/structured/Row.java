package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable representation of a single row belonging to a
 * {@link TableSchematic}.
 *
 * <p>
 * A row stores its values keyed by column name. Values are kept as raw
 * {@link Object} references; the schema is consulted on every read to perform
 * a typed {@code get} or a default-aware {@code getOrDefault}. Unknown column
 * names result in an {@link IllegalArgumentException}; missing column values
 * result in {@code null}.
 * </p>
 *
 * <p>
 * Rows are intended to be created by data back-ends (read paths) as well as
 * manually by callers that want to feed values into the API. The companion
 * builder {@link RowValues} offers schema-aware validation for write paths.
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public final class Row {

    private final @NotNull TableSchematic schema;
    private final @NotNull Map<String, Object> values;

    /**
     * Creates a new row from a value map. The supplied map is copied into a
     * new {@link LinkedHashMap} so the resulting row is fully immutable and
     * independent of subsequent changes to {@code values}.
     *
     * @param schema the schema this row belongs to.
     * @param values the column values keyed by column name. May contain keys
     *               not present in the schema – they are preserved but can
     *               not be retrieved via the typed {@code get} accessors.
     */
    public Row(@NotNull TableSchematic schema, @NotNull Map<String, Object> values) {
        this.schema = Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(values, "values");
        Map<String, Object> copy = new LinkedHashMap<>(values.size());
        copy.putAll(values);
        this.values = Collections.unmodifiableMap(copy);
    }

    /**
     * @return the schema this row is bound to.
     */
    public @NotNull TableSchematic getSchema() {
        return schema;
    }

    /**
     * Returns the value of {@code column} as the schema's declared Java type.
     *
     * <p>
     * The implementation inspects the column's {@link ColumnType} and casts
     * the stored value. The cast is unchecked at the JVM level because
     * {@code ColumnType} is parameterised – callers are expected to rely on
     * schema knowledge when choosing the target type. {@code null} is
     * returned if the column has no value in this row.
     * </p>
     *
     * @param column the column name.
     * @param <T>    the expected Java type, inferred from
     *               {@link ColumnDefinition#getType()}.
     * @return the value, or {@code null} if absent.
     * @throws IllegalArgumentException if {@code column} is not part of the
     *                                  schema.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(@NotNull String column) {
        requireColumn(column);
        Object raw = values.get(column);
        if (raw == null) {
            return null;
        }
        ColumnType<?> type = schema.getColumn(column).getType();
        if (!type.getJavaType().isInstance(raw)) {
            throw new IllegalStateException(
                    "Stored value of column '" + column + "' is of type "
                            + raw.getClass().getName()
                            + " but schema declares " + type.getJavaType().getName());
        }
        return (T) raw;
    }

    /**
     * Returns the value of {@code column} coerced to {@code expectedType}.
     * The conversion delegates to
     * {@link ColumnType#convert(Object)} and therefore honours the same
     * rules used during write-path validation. A {@code null} stored value
     * is propagated.
     *
     * @param column       the column name.
     * @param expectedType the expected column type.
     * @param <T>          the target Java type.
     * @return the converted value, or {@code null} if absent.
     * @throws IllegalArgumentException if the column is unknown or the stored
     *                                  value cannot be converted.
     */
    public <T> @Nullable T get(@NotNull String column, @NotNull ColumnType<T> expectedType) {
        requireColumn(column);
        Objects.requireNonNull(expectedType, "expectedType");
        Object raw = values.get(column);
        if (raw == null) {
            return null;
        }
        return expectedType.convert(raw);
    }

    /**
     * Returns the value of {@code column} or {@code defaultValue} if the
     * column has no value in this row. If the stored value is non-null but
     * not an instance of the default's Java type, the value is returned as
     * is without coercion.
     *
     * @param column       the column name.
     * @param defaultValue the value to return if absent.
     * @param <T>          the target Java type.
     * @return the stored value, or {@code defaultValue} if absent.
     */
    public <T> @Nullable T get(@NotNull String column, @Nullable T defaultValue) {
        requireColumn(column);
        Object raw = values.get(column);
        if (raw == null) {
            return defaultValue;
        }
        @SuppressWarnings("unchecked")
        T cast = (T) raw;
        return cast;
    }

    /**
     * Convenience accessor for {@code INT} columns.
     *
     * @throws IllegalArgumentException for an unknown column.
     */
    public @Nullable Integer getInt(@NotNull String column) {
        return get(column, ColumnType.INT);
    }

    /**
     * Convenience accessor for {@code LONG} columns.
     */
    public @Nullable Long getLong(@NotNull String column) {
        return get(column, ColumnType.LONG);
    }

    /**
     * Convenience accessor for {@code DOUBLE} columns.
     */
    public @Nullable Double getDouble(@NotNull String column) {
        return get(column, ColumnType.DOUBLE);
    }

    /**
     * Convenience accessor for {@code BOOLEAN} columns.
     */
    public @Nullable Boolean getBoolean(@NotNull String column) {
        return get(column, ColumnType.BOOLEAN);
    }

    /**
     * Convenience accessor for {@code VARCHAR}/{@code TEXT}/{@code LONGTEXT}
     * columns. Any string-producing column can be read with this method.
     */
    public @Nullable String getString(@NotNull String column) {
        return get(column, ColumnType.TEXT);
    }

    /**
     * @return {@code true} if a value for {@code column} is present,
     *         regardless of whether that value is {@code null}.
     */
    public boolean has(@NotNull String column) {
        requireColumn(column);
        return values.containsKey(column);
    }

    /**
     * Alias for {@link #has(String)} mirroring the
     * {@link java.util.Collection#contains(Object)} naming convention.
     */
    public boolean contains(@NotNull String column) {
        return has(column);
    }

    /**
     * @return an unmodifiable snapshot of the row's values keyed by column
     *         name.
     */
    public @NotNull Map<String, Object> asMap() {
        return values;
    }

    /**
     * @return an unmodifiable set of column names that have a value in this
     *         row.
     */
    public @NotNull Set<String> columns() {
        return values.keySet();
    }

    private void requireColumn(@NotNull String column) {
        Objects.requireNonNull(column, "column");
        if (schema.getColumn(column) == null) {
            throw new IllegalArgumentException(
                    "Row of table '" + schema.getName()
                            + "' has no column '" + column + "'");
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Row that)) {
            return false;
        }
        return this.schema.equals(that.schema)
                && this.values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, values);
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder("Row[");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.append(']').toString();
    }
}
