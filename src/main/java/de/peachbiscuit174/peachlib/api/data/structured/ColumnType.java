package de.peachbiscuit174.peachlib.api.data.structured;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable description of a single column type supported by the structured data system.
 * <p>
 * A {@code ColumnType} is generic over the Java type used to represent values of this
 * column in memory. The following kinds are supported:
 * </p>
 * <ul>
 *     <li>{@link Kind#INT} – fixed-width 32-bit integer ({@link Integer}).</li>
 *     <li>{@link Kind#LONG} – fixed-width 64-bit integer ({@link Long}).</li>
 *     <li>{@link Kind#DOUBLE} – 64-bit floating point ({@link Double}).</li>
 *     <li>{@link Kind#BOOLEAN} – boolean value ({@link Boolean}).</li>
 *     <li>{@link Kind#VARCHAR} – variable-length Unicode string ({@link String}) with a
 *         configurable maximum length between 1 and {@value #MAX_VARCHAR_LENGTH}.</li>
 *     <li>{@link Kind#TEXT} – variable-length Unicode string ({@link String}) of effectively
 *         unlimited size.</li>
 *     <li>{@link Kind#LONGTEXT} – variable-length Unicode string ({@link String}) reserved
 *         for very large payloads.</li>
 * </ul>
 *
 * <p>Instances are obtained through one of the predefined constants ({@link #INT},
 * {@link #LONG}, {@link #DOUBLE}, {@link #BOOLEAN}, {@link #TEXT}, {@link #LONGTEXT}) or
 * through the factory method {@link #VARCHAR(int)} for sized strings.</p>
 *
 * @param <T> the Java type used to represent values of this column
 * @author peachbiscuit174
 * @since 1.0.0
 */
public final class ColumnType<T> {

    /**
     * The maximum permitted length for a {@link Kind#VARCHAR} column.
     */
    public static final int MAX_VARCHAR_LENGTH = 16383;

    /**
     * The maximum permitted length for a {@link Kind#VARCHAR} column that is used
     * as a primary key. Some database backends impose tighter limits on the maximum
     * size of an indexed column, so a separate, smaller constant is exposed.
     */
    public static final int MAX_PRIMARY_KEY_VARCHAR_LENGTH = 768;

    /**
     * Enumerates every column kind recognised by the structured data system.
     */
    public enum Kind {
        INT,
        LONG,
        DOUBLE,
        BOOLEAN,
        VARCHAR,
        TEXT,
        LONGTEXT
    }

    /** 32-bit signed integer column. */
    public static final ColumnType<Integer> INT = new ColumnType<>(Kind.INT, Integer.class, 0);

    /** 64-bit signed integer column. */
    public static final ColumnType<Long> LONG = new ColumnType<>(Kind.LONG, Long.class, 0);

    /** 64-bit floating point column. */
    public static final ColumnType<Double> DOUBLE = new ColumnType<>(Kind.DOUBLE, Double.class, 0);

    /** Boolean column. */
    public static final ColumnType<Boolean> BOOLEAN = new ColumnType<>(Kind.BOOLEAN, Boolean.class, 0);

    /** Variable-length text column of effectively unlimited size. */
    public static final ColumnType<String> TEXT = new ColumnType<>(Kind.TEXT, String.class, 0);

    /** Variable-length text column reserved for very large payloads. */
    public static final ColumnType<String> LONGTEXT = new ColumnType<>(Kind.LONGTEXT, String.class, 0);

    private final Kind kind;
    private final Class<T> javaType;
    private final int maxLength;

    private ColumnType(@NotNull Kind kind, @NotNull Class<T> javaType, int maxLength) {
        this.kind = kind;
        this.javaType = javaType;
        this.maxLength = maxLength;
    }

    /**
     * Creates a {@link Kind#VARCHAR} column with the given maximum length.
     *
     * @param maxLength the maximum number of Unicode code points the column may store.
     *                  Must be between {@code 1} and {@link #MAX_VARCHAR_LENGTH} inclusive.
     * @return a {@code ColumnType<String>} representing the configured VARCHAR column
     * @throws IllegalArgumentException if {@code maxLength} is outside the allowed range
     */
    public static @NotNull ColumnType<String> VARCHAR(int maxLength) {
        if (maxLength < 1 || maxLength > MAX_VARCHAR_LENGTH) {
            throw new IllegalArgumentException(
                    "VARCHAR maxLength must be between 1 and " + MAX_VARCHAR_LENGTH
                            + " code points, but was " + maxLength);
        }
        return new ColumnType<>(Kind.VARCHAR, String.class, maxLength);
    }

    /**
     * @return the {@link Kind} of this column type.
     */
    public @NotNull Kind getKind() {
        return kind;
    }

    /**
     * @return the Java {@link Class} used to represent values of this column.
     */
    public @NotNull Class<T> getJavaType() {
        return javaType;
    }

    /**
     * @return the configured maximum length in Unicode code points, or {@code 0} if the
     *         kind does not carry an explicit length.
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * @return {@code true} if this column stores a numeric value
     *         ({@link Kind#INT}, {@link Kind#LONG}, {@link Kind#DOUBLE}).
     */
    public boolean isNumeric() {
        return kind == Kind.INT || kind == Kind.LONG || kind == Kind.DOUBLE;
    }

    /**
     * @return {@code true} if this column stores a text value
     *         ({@link Kind#VARCHAR}, {@link Kind#TEXT}, {@link Kind#LONGTEXT}).
     */
    public boolean isText() {
        return kind == Kind.VARCHAR || kind == Kind.TEXT || kind == Kind.LONGTEXT;
    }

    /**
     * @return {@code true} if this column kind supports automatic incrementing when used
     *         as a primary key (currently {@link Kind#INT} and {@link Kind#LONG}).
     */
    public boolean supportsAutoIncrement() {
        return kind == Kind.INT || kind == Kind.LONG;
    }

    /**
     * @return {@code true} if this column kind can serve as a primary key. Allowed kinds
     *         are {@link Kind#INT}, {@link Kind#LONG} and {@link Kind#VARCHAR} with a
     *         maximum length not exceeding {@link #MAX_PRIMARY_KEY_VARCHAR_LENGTH}.
     */
    public boolean supportsPrimaryKey() {
        if (kind == Kind.INT || kind == Kind.LONG) {
            return true;
        }
        return kind == Kind.VARCHAR && maxLength <= MAX_PRIMARY_KEY_VARCHAR_LENGTH;
    }

    /**
     * Converts an arbitrary {@link Object} into this column's Java type.
     * <p>
     * Conversion rules:
     * </p>
     * <ul>
     *     <li>{@code null} always fails.</li>
     *     <li>Numeric kinds accept any {@link Number} as long as the conversion is
     *         lossless. {@code Integer} → {@code Long} is permitted; a {@code Double}
     *         such as {@code 2.5} cannot be converted to {@code INT}; a {@code Long}
     *         outside of {@link Integer#MIN_VALUE}..{@link Integer#MAX_VALUE} cannot be
     *         converted to {@code INT}.</li>
     *     <li>{@link Kind#BOOLEAN} accepts {@link Boolean} values only.</li>
     *     <li>Text kinds accept any non-null value and return its string representation.</li>
     * </ul>
     *
     * @param value the value to convert
     * @return the converted value of type {@code T}
     * @throws IllegalArgumentException if the value cannot be converted losslessly
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public @NotNull T convert(@Nullable Object value) {
        Objects.requireNonNull(value, "Cannot convert null to " + kind);

        switch (kind) {
            case INT:
                return (T) convertToInt(value);
            case LONG:
                return (T) convertToLong(value);
            case DOUBLE:
                return (T) convertToDouble(value);
            case BOOLEAN:
                return (T) convertToBoolean(value);
            case VARCHAR:
            case TEXT:
            case LONGTEXT:
                return (T) convertToText(value);
            default:
                throw new IllegalStateException("Unhandled column kind: " + kind);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    private static Integer convertToInt(@NotNull Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Long l) {
            if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Long value " + l + " is outside the INT range ["
                                + Integer.MIN_VALUE + ", " + Integer.MAX_VALUE + "]");
            }
            return l.intValue();
        }
        if (value instanceof Short s) {
            return s.intValue();
        }
        if (value instanceof Byte b) {
            return b.intValue();
        }
        if (value instanceof Float f) {
            if (!isIntegral(f)) {
                throw new IllegalArgumentException(
                        "Double value " + f + " is not an integer and cannot be stored in INT");
            }
            int asInt = (int) f.doubleValue();
            if ((float) asInt != f.floatValue()) {
                throw new IllegalArgumentException(
                        "Floating point value " + f + " loses precision when stored in INT");
            }
            return asInt;
        }
        if (value instanceof Double d) {
            if (!isIntegral(d)) {
                throw new IllegalArgumentException(
                        "Double value " + d + " is not an integer and cannot be stored in INT");
            }
            long asLong = (long) d.doubleValue();
            if (asLong < Integer.MIN_VALUE || asLong > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Double value " + d + " is outside the INT range ["
                                + Integer.MIN_VALUE + ", " + Integer.MAX_VALUE + "]");
            }
            return (int) asLong;
        }
        if (value instanceof Number n) {
            throw new IllegalArgumentException(
                    "Number of type " + n.getClass().getName()
                            + " cannot be losslessly converted to INT");
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getName() + " to INT");
    }

    private static Long convertToLong(@NotNull Object value) {
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Short s) {
            return s.longValue();
        }
        if (value instanceof Byte b) {
            return b.longValue();
        }
        if (value instanceof Float f) {
            if (!isIntegral(f)) {
                throw new IllegalArgumentException(
                        "Float value " + f + " is not an integer and cannot be stored in LONG");
            }
            return (long) f.doubleValue();
        }
        if (value instanceof Double d) {
            if (!isIntegral(d)) {
                throw new IllegalArgumentException(
                        "Double value " + d + " is not an integer and cannot be stored in LONG");
            }
            return (long) d.doubleValue();
        }
        if (value instanceof Number) {
            throw new IllegalArgumentException(
                    "Number of type " + value.getClass().getName()
                            + " cannot be losslessly converted to LONG");
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getName() + " to LONG");
    }

    private static Double convertToDouble(@NotNull Object value) {
        if (value instanceof Double d) {
            return d;
        }
        if (value instanceof Float f) {
            return f.doubleValue();
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getName() + " to DOUBLE");
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Boolean convertToBoolean(@NotNull Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException(
                "Cannot convert " + value.getClass().getName() + " to BOOLEAN");
    }

    @SuppressWarnings("unchecked")
    private static <T> @NotNull T convertToText(@NotNull Object value) {
        return cast(value.toString());
    }

    private static boolean isIntegral(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d) && d == Math.floor(d);
    }

    private static boolean isIntegral(float f) {
        return !Float.isNaN(f) && !Float.isInfinite(f) && f == Math.floor(f);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ColumnType<?> that)) {
            return false;
        }
        return this.kind == that.kind && this.maxLength == that.maxLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, maxLength);
    }

    @Override
    public @NotNull String toString() {
        if (kind == Kind.VARCHAR) {
            return "VARCHAR(" + maxLength + ")";
        }
        return kind.name();
    }
}
