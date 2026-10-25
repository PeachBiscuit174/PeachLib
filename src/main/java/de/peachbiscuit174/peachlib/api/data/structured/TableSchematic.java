package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Immutable, ordered description of a database table managed by the structured data
 * system. A schematic consists of a {@link #getName() name} and an ordered list of
 * {@link ColumnDefinition}s. It does not, by itself, perform any database operations –
 * it merely describes the shape that storage backends are expected to honour.
 * <p>
 * Instances are constructed via {@link #builder(String)} which returns a fluent
 * {@link Builder}. Once {@link Builder#build()} has been called the resulting
 * {@code TableSchematic} is immutable and safe to share across threads.
 * </p>
 * <p>
 * For convenience the {@link #configure(Consumer)} entry point can be passed to API
 * methods that accept a {@link Consumer Consumer&lt;TableSchematic.Builder&gt;}, e.g.:
 * </p>
 * <pre>{@code
 * manager.getTable(storage, "users", TableSchematic.configure(t -> t
 *         .column("id", ColumnType.LONG)
 *             .primaryKey()
 *             .autoIncrement()
 *         .column("name", ColumnType.VARCHAR(64))
 *             .nullable()
 *         .build()));
 * }</pre>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public final class TableSchematic {

    /**
     * Regular expression applied to both table and column names. Names must start with
     * a lower-case letter and may contain lower-case letters, digits, and underscores.
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");

    /**
     * Maximum length of a table name, in characters.
     */
    public static final int MAX_NAME_LENGTH = 47;

    private final String name;
    private final List<ColumnDefinition> columns;

    private TableSchematic(@NotNull String name, @NotNull List<ColumnDefinition> columns) {
        this.name = name;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
    }

    /**
     * Starts building a schematic for a table with the given {@code name}.
     *
     * @param name the desired table name; must satisfy {@link #NAME_PATTERN} and be at
     *             most {@link #MAX_NAME_LENGTH} characters long.
     * @return a fluent {@link Builder} ready to receive column definitions.
     * @throws IllegalArgumentException if {@code name} is invalid.
     */
    public static @NotNull Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    /**
     * Convenience overload that allows callers to write
     * {@code TableSchematic.configure(t -> t.column(...))} without explicitly creating a
     * builder. The returned {@link Consumer} is intended for API entry points that take
     * a {@code Consumer<TableSchematic.Builder>}.
     *
     * @param configurer the consumer applied to the new builder.
     * @return the same configurer – returned unchanged so the call site reads naturally.
     */
    public static @NotNull Consumer<Builder> configure(@NotNull Consumer<Builder> configurer) {
        Objects.requireNonNull(configurer, "configurer");
        return configurer;
    }

    /**
     * @return the table name. Guaranteed to satisfy {@link #NAME_PATTERN} and to be at
     *         most {@link #MAX_NAME_LENGTH} characters long.
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * @return an immutable, ordered view of the columns in this schematic.
     */
    public @NotNull List<ColumnDefinition> getColumns() {
        return columns;
    }

    /**
     * @return the column flagged as primary key.
     * @throws IllegalStateException if this schematic does not contain exactly one primary key.
     */
    public @NotNull ColumnDefinition getPrimaryKey() {
        ColumnDefinition pk = null;
        for (ColumnDefinition column : columns) {
            if (column.isPrimaryKey()) {
                if (pk != null) {
                    throw new IllegalStateException(
                            "Table '" + name + "' has more than one primary key: "
                                    + pk.getName() + " and " + column.getName());
                }
                pk = column;
            }
        }
        if (pk == null) {
            throw new IllegalStateException("Table '" + name + "' has no primary key");
        }
        return pk;
    }

    /**
     * @param columnName the column to look up.
     * @return the matching {@link ColumnDefinition} or {@code null} if the schematic
     *         contains no column with that name.
     */
    public @Nullable ColumnDefinition getColumn(@NotNull String columnName) {
        for (ColumnDefinition column : columns) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }
        return null;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TableSchematic that)) {
            return false;
        }
        return this.name.equals(that.name) && this.columns.equals(that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns);
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder("TableSchematic{name=").append(name).append(", columns=[");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i));
        }
        return sb.append("]}").toString();
    }

    /**
     * Fluent builder for {@link TableSchematic}s. Every chain must begin with at least
     * one {@link #column(String, ColumnType)} call and is terminated by
     * {@link #build()}. Modifier calls such as {@link #primaryKey()} may only appear
     * <em>after</em> the {@code column(...)} call they belong to.
     */
    public static final class Builder {

        private final String name;
        private final List<ColumnDefinition> columns = new ArrayList<>();
        private final List<String> seenNames = new ArrayList<>();

        private boolean nextIsNullable = false;
        private boolean nextIsPrimaryKey = false;
        private boolean nextIsAutoIncrement = false;
        private Object nextDefaultValue = null;
        private boolean nextHasDefaultValue = false;
        private boolean pendingModifiers = false;

        private Builder(@NotNull String name) {
            validateName(name, "Table name");
            this.name = name;
        }

        /**
         * Starts a new column definition. Any modifiers configured on the previous
         * column are validated and committed before this method returns.
         *
         * @param columnName the column name. Must satisfy {@link #NAME_PATTERN} and be
         *                   at most {@link ColumnDefinition#MAX_NAME_LENGTH} characters.
         * @param type       the column type.
         * @return this builder, ready to receive modifiers for the freshly started
         *         column.
         * @throws IllegalArgumentException if {@code columnName} or {@code type} is invalid,
         *                                  or if a column with the same name already exists.
         */
        public @NotNull Builder column(@NotNull String columnName, @NotNull ColumnType<?> type) {
            commitPendingColumn();

            validateName(columnName, "Column name");

            for (String existing : seenNames) {
                if (existing.equals(columnName)) {
                    throw new IllegalArgumentException(
                            "Duplicate column name '" + columnName
                                    + "' in table '" + name + "'");
                }
            }

            Objects.requireNonNull(type, "Column type");

            nextIsNullable = false;
            nextIsPrimaryKey = false;
            nextIsAutoIncrement = false;
            nextDefaultValue = null;
            nextHasDefaultValue = false;
            pendingModifiers = true;

            // Remember the pending column name and type until commitPendingColumn().
            pendingColumnName = columnName;
            pendingColumnType = type;
            return this;
        }

        // State used to remember the column currently under construction.
        private @Nullable String pendingColumnName = null;
        private @Nullable ColumnType<?> pendingColumnType = null;

        /**
         * Marks the current column as the table's primary key. Implies {@code NOT NULL}.
         * <p>
         * Allowed primary key types are {@link ColumnType.Kind#INT},
         * {@link ColumnType.Kind#LONG} and {@link ColumnType.Kind#VARCHAR} with a
         * maximum length not exceeding {@link ColumnType#MAX_PRIMARY_KEY_VARCHAR_LENGTH}.
         * Other kinds, or VARCHAR columns longer than that limit, are rejected at
         * column commit time (i.e. when the next {@link #column(String, ColumnType)}
         * call begins, or when {@link #build()} is invoked) – the order of modifier
         * calls does not matter.
         * </p>
         *
         * @return this builder.
         * @throws IllegalStateException    if called before any {@link #column(String, ColumnType)}
         *                                  call.
         * @throws IllegalArgumentException at column commit time if the chosen column
         *                                  type cannot serve as a primary key.
         */
        public @NotNull Builder primaryKey() {
            ensureModifiersStarted("primaryKey()");
            nextIsPrimaryKey = true;
            // PK columns are always NOT NULL.
            nextIsNullable = false;
            return this;
        }

        /**
         * Marks the current column as auto-incrementing. Requires the column's type to
         * support auto-increment and the column to be the primary key.
         *
         * @return this builder.
         * @throws IllegalStateException if called before any {@link #column(String, ColumnType)}
         *                               call.
         */
        public @NotNull Builder autoIncrement() {
            ensureModifiersStarted("autoIncrement()");
            nextIsAutoIncrement = true;
            return this;
        }

        /**
         * Marks the current column as nullable.
         *
         * @return this builder.
         * @throws IllegalStateException if called before any {@link #column(String, ColumnType)}
         *                               call.
         */
        public @NotNull Builder nullable() {
            ensureModifiersStarted("nullable()");
            nextIsNullable = true;
            return this;
        }

        /**
         * Sets the default value of the current column. The value is immediately
         * converted via {@link ColumnType#convert(Object)} so the final
         * {@link ColumnDefinition} always holds a value that matches the column's Java
         * type.
         *
         * @param value the raw default value; must not be {@code null}.
         * @return this builder.
         * @throws IllegalArgumentException if the value cannot be converted to the
         *                                  column's type, or if it is {@code null}.
         * @throws IllegalStateException    if called before any {@link #column(String, ColumnType)}
         *                                  call.
         */
        public @NotNull Builder defaultValue(@NotNull Object value) {
            ensureModifiersStarted("defaultValue(value)");
            Objects.requireNonNull(value, "Default value must not be null");
            if (pendingColumnType == null) {
                // Defensive – should never happen because ensureModifiersStarted requires it.
                throw new IllegalStateException("No column type available for default value");
            }
            Object converted = pendingColumnType.convert(value);
            nextDefaultValue = converted;
            nextHasDefaultValue = true;
            return this;
        }

        /**
         * Finalises the builder and returns the immutable {@link TableSchematic}.
         *
         * @return the built schematic.
         * @throws IllegalStateException    if no columns have been defined, or if a
         *                                  primary-key violation is detected.
         * @throws IllegalArgumentException if any column definition is invalid.
         */
        public @NotNull TableSchematic build() {
            commitPendingColumn();

            if (columns.isEmpty()) {
                throw new IllegalStateException(
                        "Table '" + name + "' must have at least one column");
            }

            // Validate primary-key constraints: exactly one, correct type, NOT NULL.
            ColumnDefinition pk = null;
            for (ColumnDefinition column : columns) {
                if (column.isPrimaryKey()) {
                    if (pk != null) {
                        throw new IllegalStateException(
                                "Table '" + name + "' defines more than one primary key: "
                                        + pk.getName() + " and " + column.getName());
                    }
                    if (!column.getType().supportsPrimaryKey()) {
                        if (column.getType().getKind() == ColumnType.Kind.VARCHAR) {
                            throw new IllegalArgumentException(
                                    "Primary key column '" + column.getName() + "' in table '"
                                            + name + "' uses " + column.getType()
                                            + ", but VARCHAR primary keys may have at most "
                                            + ColumnType.MAX_PRIMARY_KEY_VARCHAR_LENGTH
                                            + " characters");
                        }
                        throw new IllegalArgumentException(
                                "Primary key column '" + column.getName() + "' in table '"
                                        + name + "' uses unsupported type "
                                        + column.getType()
                                        + "; allowed primary key types are INT, LONG and VARCHAR "
                                        + "(with at most " + ColumnType.MAX_PRIMARY_KEY_VARCHAR_LENGTH
                                        + " characters)");
                    }
                    if (column.isNullable()) {
                        throw new IllegalArgumentException(
                                "Primary key column '" + column.getName() + "' in table '"
                                        + name + "' must be NOT NULL");
                    }
                    if (column.isAutoIncrement() && !column.getType().supportsAutoIncrement()) {
                        throw new IllegalArgumentException(
                                "Auto-increment on column '" + column.getName()
                                        + "' requires an INT or LONG type, but was "
                                        + column.getType());
                    }
                    pk = column;
                }
            }
            if (pk == null) {
                throw new IllegalStateException(
                        "Table '" + name + "' must define exactly one primary key");
            }

            return new TableSchematic(name, columns);
        }

        private void ensureModifiersStarted(@NotNull String method) {
            if (!pendingModifiers) {
                throw new IllegalStateException(
                        method + " called before any column() on table '" + name + "'");
            }
        }

        private void commitPendingColumn() {
            if (!pendingModifiers) {
                return;
            }

            String columnName = pendingColumnName;
            ColumnType<?> columnType = pendingColumnType;

            // Reset pending state up front so subsequent validation errors do not leave
            // the builder in a half-configured state.
            pendingModifiers = false;
            pendingColumnName = null;
            pendingColumnType = null;

            if (columnName == null || columnType == null) {
                // Should never happen – defensive guard.
                throw new IllegalStateException("Internal builder state is inconsistent");
            }

            // Validate against PK + autoIncrement + default combinations.
            if (nextIsPrimaryKey && nextIsNullable) {
                throw new IllegalArgumentException(
                        "Primary key column '" + columnName
                                + "' in table '" + name + "' must be NOT NULL");
            }
            if (nextIsAutoIncrement && !columnType.supportsAutoIncrement()) {
                throw new IllegalArgumentException(
                        "Auto-increment on column '" + columnName
                                + "' requires an INT or LONG type, but was " + columnType);
            }
            if (nextIsAutoIncrement && nextHasDefaultValue) {
                throw new IllegalArgumentException(
                        "Column '" + columnName
                                + "' cannot combine autoIncrement() with defaultValue()");
            }
            if (nextIsPrimaryKey && nextHasDefaultValue) {
                throw new IllegalArgumentException(
                        "Primary key column '" + columnName + "' in table '" + name
                                + "' must not have a default value");
            }
            if (nextIsPrimaryKey && !columnType.supportsPrimaryKey()) {
                if (columnType.getKind() == ColumnType.Kind.VARCHAR) {
                    throw new IllegalArgumentException(
                            "Primary key column '" + columnName + "' in table '" + name
                                    + "' uses " + columnType
                                    + ", but VARCHAR primary keys may have at most "
                                    + ColumnType.MAX_PRIMARY_KEY_VARCHAR_LENGTH
                                    + " characters");
                }
                throw new IllegalArgumentException(
                        "Primary key column '" + columnName + "' in table '" + name
                                + "' uses unsupported type " + columnType
                                + "; allowed primary key types are INT, LONG and VARCHAR "
                                + "(with at most " + ColumnType.MAX_PRIMARY_KEY_VARCHAR_LENGTH
                                + " characters)");
            }

            ColumnDefinition definition = new ColumnDefinition(
                    columnName,
                    columnType,
                    nextIsNullable,
                    nextIsPrimaryKey,
                    nextIsAutoIncrement,
                    nextHasDefaultValue ? nextDefaultValue : null);

            columns.add(definition);
            seenNames.add(columnName);

            // Reset modifier state for the next column.
            nextIsNullable = false;
            nextIsPrimaryKey = false;
            nextIsAutoIncrement = false;
            nextDefaultValue = null;
            nextHasDefaultValue = false;
        }

        private static void validateName(@NotNull String value, @NotNull String description) {
            if (!NAME_PATTERN.matcher(value).matches()) {
                throw new IllegalArgumentException(
                        description + " '" + value
                                + "' must match ^[a-z][a-z0-9_]*$");
            }
            int max = description.equals("Table name") ? MAX_NAME_LENGTH : ColumnDefinition.MAX_NAME_LENGTH;
            // Count code points instead of UTF-16 code units so that emoji and other
            // supplementary characters do not silently exceed the limit.
            if (value.codePointCount(0, value.length()) > max) {
                throw new IllegalArgumentException(
                        description + " '" + value + "' exceeds maximum length of "
                                + max + " code points");
            }
        }
    }
}
