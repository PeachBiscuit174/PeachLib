package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Immutable, schema-validated query filter used by the structured data system.
 *
 * <p>
 * A {@code Filter} is built through the static factory methods such as
 * {@link #eq(String, Object)} or through the fluent column-scoped builder
 * returned by {@link #where(String)}. The resulting instance is one of a small
 * set of {@link Filter sealed} record types so that back-ends can translate it
 * idiomatically via pattern matching: a SQL adapter can produce a parameterised
 * {@code WHERE} clause, an in-memory adapter can call {@link #matches(Map)}
 * directly.
 * </p>
 *
 * <p>
 * A {@code Filter} is validated against a {@link TableSchematic} via
 * {@link #validate(TableSchematic)} which performs the following checks:
 * </p>
 * <ul>
 *     <li>The referenced column must exist.</li>
 *     <li>The supplied value must be convertible to the column's
 *         {@link ColumnType} (delegated to {@link ColumnType#convert(Object)});
 *         the conversion has to be lossless, so incompatible numeric types are
 *         rejected as well, e.g. {@code 2.5} on a {@link ColumnType.Kind#LONG}
 *         column raises an {@link IllegalArgumentException} instead of being
 *         truncated or rounded.</li>
 *     <li>{@code gt}/{@code gte}/{@code lt}/{@code lte} are forbidden on
 *         {@link ColumnType.Kind#BOOLEAN} columns.</li>
 *     <li>{@code eq} on a {@code null} value is rejected – use {@link #isNull}
 *         instead.</li>
 *     <li>{@link #like} is only valid on text columns
 *         ({@link ColumnType.Kind#VARCHAR}, {@link ColumnType.Kind#TEXT},
 *         {@link ColumnType.Kind#LONGTEXT}).</li>
 * </ul>
 *
 * <p>
 * The {@link #matches(Map)} method implements SQL-style three-free semantics:
 * any comparison involving a {@code null} operand evaluates to {@code false},
 * except for {@link #isNull} and {@link #isNotNull} which inspect the operand
 * directly. An empty {@link #in} collection is defined to match nothing
 * (always {@code false}).
 * </p>
 *
 * @author peachbiscuit174
 * @since 1.0.0
 */
public sealed interface Filter permits Filter.All, Filter.Comparison, Filter.In, Filter.Like,
        Filter.Not, Filter.And, Filter.Or {

    /**
     * Validates this filter against the supplied {@link TableSchematic}.
     *
     * <p>
     * The check is type-strict: every supplied value has to be convertible
     * losslessly to the column type via {@link ColumnType#convert(Object)}.
     * Values whose numeric type does not fit the column type are rejected as
     * well: a fractional number such as {@code 2.5} supplied for a
     * {@link ColumnType.Kind#LONG} or {@link ColumnType.Kind#INT} column
     * raises an {@link IllegalArgumentException} instead of being truncated or
     * rounded. The {@code in} filter validates every element of the supplied
     * collection individually.
     * </p>
     *
     * @param schema the schema to validate against.
     * @throws IllegalArgumentException if the filter references an unknown
     *                                  column, a value cannot be converted to
     *                                  the column's type (for example a
     *                                  fractional value such as {@code 2.5} for
     *                                  a {@link ColumnType.Kind#LONG} or
     *                                  {@link ColumnType.Kind#INT} column), a
     *                                  comparison is illegal for the column
     *                                  type or a {@code null} value is
     *                                  supplied for {@link Op#EQ} /
     *                                  {@link Op#NOT_EQ}.
     */
    void validate(@NotNull TableSchematic schema);

    /**
     * Evaluates this filter against the supplied value map. The map must be
     * keyed by column name and contain the raw values (not yet converted) of
     * a candidate row.
     *
     * @param values the row values, keyed by column name.
     * @return {@code true} if the candidate row matches the filter.
     */
    boolean matches(@NotNull Map<String, Object> values);

    /**
     * Operator used by {@link Comparison}.
     */
    enum Op {
        EQ, NOT_EQ,
        GT, GTE, LT, LTE,
        IS_NULL, IS_NOT_NULL
    }

    /**
     * @return a filter that matches every row.
     */
    static @NotNull Filter all() {
        return new All();
    }

    /**
     * Begins a column-scoped fluent builder. The returned object is not a
     * {@code Filter} yet; call one of its methods to obtain one.
     *
     * @param column the column name to filter on.
     * @return a column-scoped builder.
     */
    static @NotNull ColumnRef where(@NotNull String column) {
        return new ColumnRef(column);
    }

    /**
     * @return a filter equivalent to {@code col = value}.
     */
    static @NotNull Filter eq(@NotNull String column, @Nullable Object value) {
        return new Comparison(column, Op.EQ, value);
    }

    /**
     * @return a filter equivalent to {@code col &lt;&gt; value}.
     */
    static @NotNull Filter notEq(@NotNull String column, @Nullable Object value) {
        return new Comparison(column, Op.NOT_EQ, value);
    }

    /**
     * @return a filter equivalent to {@code col &gt; value}.
     */
    static @NotNull Filter greaterThan(@NotNull String column, @Nullable Object value) {
        return new Comparison(column, Op.GT, value);
    }

    /**
     * @return a filter equivalent to {@code col &gt;= value}.
     */
    static @NotNull Filter greaterOrEqual(@NotNull String column, @Nullable Object value) {
        return new Comparison(column, Op.GTE, value);
    }

    /**
     * @return a filter equivalent to {@code col &lt; value}.
     */
    static @NotNull Filter lessThan(@NotNull String column, @Nullable Object value) {
        return new Comparison(column, Op.LT, value);
    }

    /**
     * @return a filter equivalent to {@code col &lt;= value}.
     */
    static @NotNull Filter lessOrEqual(@NotNull String column, @Nullable Object value) {
        return new Comparison(column, Op.LTE, value);
    }

    /**
     * @return a filter equivalent to {@code col IS NULL}.
     */
    static @NotNull Filter isNull(@NotNull String column) {
        return new Comparison(column, Op.IS_NULL, null);
    }

    /**
     * @return a filter equivalent to {@code col IS NOT NULL}.
     */
    static @NotNull Filter isNotNull(@NotNull String column) {
        return new Comparison(column, Op.IS_NOT_NULL, null);
    }

    /**
     * @return a filter equivalent to {@code col IN (values)}. An empty
     * collection is defined to match no row.
     */
    static @NotNull Filter in(@NotNull String column, @NotNull Collection<?> values) {
        return new In(column, List.copyOf(values));
    }

    /**
     * @return a filter equivalent to SQL {@code col LIKE pattern}. The
     * pattern uses SQL semantics: {@code %} matches any sequence of
     * characters, {@code _} matches a single character. The pattern is only
     * valid on text columns.
     */
    static @NotNull Filter like(@NotNull String column, @NotNull String pattern) {
        return new Like(column, pattern);
    }

    /**
     * @return the conjunction of the supplied filters. An empty array yields
     * {@link #all()}.
     */
    static @NotNull Filter and(@NotNull Filter @NotNull ... filters) {
        if (filters.length == 0) {
            return all();
        }
        if (filters.length == 1) {
            return filters[0];
        }
        return new And(Arrays.asList(filters));
    }

    /**
     * @return the disjunction of the supplied filters. An empty array yields
     * {@link #all()}.
     */
    static @NotNull Filter or(@NotNull Filter @NotNull ... filters) {
        if (filters.length == 0) {
            return all();
        }
        if (filters.length == 1) {
            return filters[0];
        }
        return new Or(Arrays.asList(filters));
    }

    /**
     * @return the logical negation of the supplied filter.
     */
    static @NotNull Filter not(@NotNull Filter filter) {
        return new Not(filter);
    }

    /**
     * Matches every row. Useful as a neutral element for {@link #and} /
     * {@link #or}.
     */
    record All() implements Filter {

        @Override
        public void validate(@NotNull TableSchematic schema) {
            // nothing to validate
        }

        @Override
        public boolean matches(@NotNull Map<String, Object> values) {
            return true;
        }

        @Override
        public @NotNull String toString() {
            return "All";
        }
    }

    /**
     * A single binary comparison against the value of one column. The
     * {@link Op#IS_NULL} and {@link Op#IS_NOT_NULL} operators ignore the
     * value.
     */
    record Comparison(@NotNull String column, @NotNull Op op, @Nullable Object value)
            implements Filter {

        @Override
        public void validate(@NotNull TableSchematic schema) {
            ColumnDefinition def = requireColumn(schema, column);
            ColumnType<?> type = def.getType();

            switch (op) {
                case IS_NULL, IS_NOT_NULL -> {
                    // no value semantics
                }
                case EQ, NOT_EQ -> {
                    if (value == null) {
                        throw new IllegalArgumentException(
                                "Filter " + op + " on column '" + column
                                        + "' does not accept null; use isNull()/isNotNull()");
                    }
                    requireConvertible(type, value);
                }
                case GT, GTE, LT, LTE -> {
                    if (type.getKind() == ColumnType.Kind.BOOLEAN) {
                        throw new IllegalArgumentException(
                                "Ordering comparison (" + op + ") is not allowed on BOOLEAN column '"
                                        + column + "'");
                    }
                    if (value == null) {
                        throw new IllegalArgumentException(
                                "Filter " + op + " on column '" + column
                                        + "' requires a non-null value");
                    }
                    requireConvertible(type, value);
                }
            }
        }

        @Override
        public boolean matches(@NotNull Map<String, Object> values) {
            Object actual = values.get(column);
            return switch (op) {
                case IS_NULL -> actual == null;
                case IS_NOT_NULL -> actual != null;
                case EQ -> actual != null && equalsTyped(actual, value);
                case NOT_EQ -> !(actual != null && equalsTyped(actual, value));
                case GT -> compareTyped(actual, value) > 0;
                case GTE -> compareTyped(actual, value) >= 0;
                case LT -> compareTyped(actual, value) < 0;
                case LTE -> compareTyped(actual, value) <= 0;
            };
        }
    }

    /**
     * Membership test. An empty value list is defined to match no row.
     */
    record In(@NotNull String column, @NotNull List<?> values) implements Filter {

        @Override
        public void validate(@NotNull TableSchematic schema) {
            ColumnDefinition def = requireColumn(schema, column);
            ColumnType<?> type = def.getType();
            for (Object v : values) {
                if (v == null) {
                    throw new IllegalArgumentException(
                            "Filter in() on column '" + column
                                    + "' does not accept null elements");
                }
                requireConvertible(type, v);
            }
        }

        @Override
        public boolean matches(@NotNull Map<String, Object> values) {
            Object actual = values.get(column);
            if (actual == null || this.values.isEmpty()) {
                return false;
            }
            for (Object candidate : this.values) {
                if (equalsTyped(actual, candidate)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * SQL {@code LIKE} filter for text columns. The pattern uses {@code %}
     * (zero-or-more characters) and {@code _} (single character) wildcards.
     */
    record Like(@NotNull String column, @NotNull String pattern) implements Filter {

        @Override
        public void validate(@NotNull TableSchematic schema) {
            ColumnDefinition def = requireColumn(schema, column);
            if (!def.getType().isText()) {
                throw new IllegalArgumentException(
                        "Filter like() on column '" + column
                                + "' requires a text column but found "
                                + def.getType().getKind());
            }
            Objects.requireNonNull(pattern, "pattern");
        }

        @Override
        public boolean matches(@NotNull Map<String, Object> values) {
            Object actual = values.get(column);
            if (!(actual instanceof String s)) {
                return false;
            }
            return likeRegex(pattern).matcher(s).matches();
        }
    }

    /**
     * Logical negation. {@code not(all())} is allowed and matches no row.
     */
    record Not(@NotNull Filter inner) implements Filter {

        @Override
        public void validate(@NotNull TableSchematic schema) {
            inner.validate(schema);
        }

        @Override
        public boolean matches(@NotNull Map<String, Object> values) {
            return !inner.matches(values);
        }
    }

    /**
     * Logical conjunction of an ordered list of filters. An empty list is not
     * permitted; build via {@link Filter#and(Filter...)} which short-circuits.
     */
    record And(@NotNull List<Filter> children) implements Filter {

        @Override
        public void validate(@NotNull TableSchematic schema) {
            for (Filter child : children) {
                child.validate(schema);
            }
        }

        @Override
        public boolean matches(@NotNull Map<String, Object> values) {
            for (Filter child : children) {
                if (!child.matches(values)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Logical disjunction of an ordered list of filters. An empty list is not
     * permitted; build via {@link Filter#or(Filter...)} which short-circuits.
     */
    record Or(@NotNull List<Filter> children) implements Filter {

        @Override
        public void validate(@NotNull TableSchematic schema) {
            for (Filter child : children) {
                child.validate(schema);
            }
        }

        @Override
        public boolean matches(@NotNull Map<String, Object> values) {
            for (Filter child : children) {
                if (child.matches(values)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Fluent column-scoped builder returned by {@link Filter#where(String)}.
     * Calling any method produces an immutable {@link Filter}.
     */
    final class ColumnRef {

        private final @NotNull String column;

        ColumnRef(@NotNull String column) {
            this.column = Objects.requireNonNull(column, "column");
        }

        public @NotNull Filter eq(@Nullable Object value) {
            return new Comparison(column, Op.EQ, value);
        }

        public @NotNull Filter notEq(@Nullable Object value) {
            return new Comparison(column, Op.NOT_EQ, value);
        }

        public @NotNull Filter greaterThan(@Nullable Object value) {
            return new Comparison(column, Op.GT, value);
        }

        public @NotNull Filter greaterOrEqual(@Nullable Object value) {
            return new Comparison(column, Op.GTE, value);
        }

        public @NotNull Filter lessThan(@Nullable Object value) {
            return new Comparison(column, Op.LT, value);
        }

        public @NotNull Filter lessOrEqual(@Nullable Object value) {
            return new Comparison(column, Op.LTE, value);
        }

        public @NotNull Filter isNull() {
            return new Comparison(column, Op.IS_NULL, null);
        }

        public @NotNull Filter isNotNull() {
            return new Comparison(column, Op.IS_NOT_NULL, null);
        }

        public @NotNull Filter in(@NotNull Collection<?> values) {
            return new In(column, List.copyOf(values));
        }

        public @NotNull Filter like(@NotNull String pattern) {
            return new Like(column, pattern);
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Looks up {@code column} in the schema and throws if missing.
     */
    private static @NotNull ColumnDefinition requireColumn(
            @NotNull TableSchematic schema, @NotNull String column) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(column, "column");
        ColumnDefinition def = schema.getColumn(column);
        if (def == null) {
            throw new IllegalArgumentException(
                    "Filter references unknown column '" + column + "' in table '"
                            + schema.getName() + "'");
        }
        return def;
    }

    /**
     * Converts {@code value} via the column type, re-throwing any conversion
     * error as a filter-validation error.
     */
    private static void requireConvertible(
            @NotNull ColumnType<?> type, @Nullable Object value) {
        try {
            type.convert(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Filter value is not convertible to column type "
                            + type + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Typed equality used by {@link #matches(Map)}: numeric values are
     * compared as {@code double}, everything else by {@link Objects#equals}.
     */
    private static boolean equalsTyped(@NotNull Object a, @Nullable Object b) {
        if (b == null) {
            return false;
        }
        if (a instanceof Number && b instanceof Number) {
            return Double.compare(((Number) a).doubleValue(),
                    ((Number) b).doubleValue()) == 0;
        }
        return a.equals(b);
    }

    /**
     * Typed comparison used by {@link #matches(Map)}. Numeric comparisons
     * delegate to {@link Long#compare(long, long)} for {@code INT}/{@code LONG}
     * columns and to {@link Double#compare(double, double)} for {@code DOUBLE}
     * columns so that large {@code long} values such as {@link Long#MAX_VALUE}
     * are ordered exactly without {@code double} rounding. Text columns are
     * compared via {@link Comparable#compareTo(Object)}. If either operand is
     * {@code null} or not comparable, returns {@code -1} so that ordering
     * comparisons evaluate to {@code false} for null operands.
     */
    private static int compareTyped(@Nullable Object a, @Nullable Object b) {
        if (a == null || b == null) {
            return -1;
        }
        if (a instanceof Long la && b instanceof Long lb) {
            return Long.compare(la, lb);
        }
        if (a instanceof Integer ia && b instanceof Integer ib) {
            return Integer.compare(ia, ib);
        }
        if (a instanceof Long la && b instanceof Integer ib) {
            return Long.compare(la, ib.longValue());
        }
        if (a instanceof Integer ia && b instanceof Long lb) {
            return Long.compare(ia.longValue(), lb);
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof Comparable<?> ca && b instanceof Comparable<?>) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            int cmp = ((Comparable) ca).compareTo(b);
            return cmp;
        }
        return -1;
    }

    /**
     * Converts a SQL {@code LIKE} pattern to a Java {@link Pattern} with
     * anchored matching.
     */
    private static @NotNull Pattern likeRegex(@NotNull String sqlPattern) {
        StringBuilder sb = new StringBuilder(sqlPattern.length() + 4);
        sb.append('^');
        for (int i = 0; i < sqlPattern.length(); i++) {
            char c = sqlPattern.charAt(i);
            switch (c) {
                case '%' -> sb.append(".*");
                case '_' -> sb.append('.');
                case '\\', '.', '+', '*', '?', '(', ')', '[', ']', '{', '}', '|',
                     '^', '$' -> sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString(), Pattern.DOTALL);
    }

    /**
     * @return an unmodifiable snapshot of the {@code values} collection.
     */
    private static <T> @NotNull Collection<T> unmodifiable(@NotNull Collection<T> values) {
        if (values instanceof Set<T> s) {
            return Collections.unmodifiableSet(new HashSet<>(s));
        }
        return Collections.unmodifiableCollection(values);
    }
}
