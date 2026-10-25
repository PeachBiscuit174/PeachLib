package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable definition of a single column as part of a {@link TableSchematic}.
 * <p>
 * Instances are created exclusively by the {@link TableSchematic.Builder} and must not
 * be mutated afterwards. Every column is characterised by:
 * </p>
 * <ul>
 *     <li>A column {@link #getName() name} matching {@code ^[a-z][a-z0-9_]*$} and at most
 *         {@value #MAX_NAME_LENGTH} characters long.</li>
 *     <li>A {@link ColumnType} describing its storage kind.</li>
 *     <li>A {@link #isNullable() nullable} flag – primary keys are always non-null.</li>
 *     <li>An {@link #isPrimaryKey() primary key} flag – exactly one column per table
 *         must be marked as primary key.</li>
 *     <li>An {@link #isAutoIncrement() auto increment} flag – only meaningful on
 *         {@link ColumnType.Kind#INT} or {@link ColumnType.Kind#LONG} primary keys.</li>
 *     <li>An optional default value that has already been converted via
 *         {@link ColumnType#convert(Object)} so it always matches the column's Java type.</li>
 * </ul>
 *
 * <p>Equality is defined over <em>all</em> attributes except the converted default
 * value's identity – the value itself is compared via {@link Objects#equals(Object, Object)}.</p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public final class ColumnDefinition {

    /**
     * Maximum length of a column name, in characters.
     */
    public static final int MAX_NAME_LENGTH = 64;

    private final String name;
    private final ColumnType<?> type;
    private final boolean nullable;
    private final boolean primaryKey;
    private final boolean autoIncrement;
    private final Object defaultValue;

    ColumnDefinition(@NotNull String name,
                     @NotNull ColumnType<?> type,
                     boolean nullable,
                     boolean primaryKey,
                     boolean autoIncrement,
                     @Nullable Object defaultValue) {
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.autoIncrement = autoIncrement;
        this.defaultValue = defaultValue;
    }

    /**
     * @return the column name. Guaranteed to match {@code ^[a-z][a-z0-9_]*$} and to be at
     *         most {@value #MAX_NAME_LENGTH} characters long.
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * @return the column's {@link ColumnType}.
     */
    public @NotNull ColumnType<?> getType() {
        return type;
    }

    /**
     * @return {@code true} if this column may store SQL {@code NULL}.
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * @return {@code true} if this column is the table's primary key.
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * @return {@code true} if this column auto-increments on insert. Only valid for
     *         primary keys with a numeric {@link ColumnType}.
     */
    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    /**
     * @return the pre-converted default value for this column, or {@code null} if the
     *         column has no explicit default.
     */
    public @Nullable Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ColumnDefinition that)) {
            return false;
        }
        return this.nullable == that.nullable
                && this.primaryKey == that.primaryKey
                && this.autoIncrement == that.autoIncrement
                && this.name.equals(that.name)
                && this.type.equals(that.type)
                && Objects.equals(this.defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, nullable, primaryKey, autoIncrement, defaultValue);
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(' ').append(type);
        if (primaryKey) {
            sb.append(" PRIMARY KEY");
            if (autoIncrement) {
                sb.append(" AUTO_INCREMENT");
            }
        }
        if (!nullable) {
            sb.append(" NOT NULL");
        }
        if (defaultValue != null) {
            sb.append(" DEFAULT ").append(defaultValue);
        }
        return sb.toString();
    }
}
