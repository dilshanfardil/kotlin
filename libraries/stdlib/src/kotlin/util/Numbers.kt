@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("MathKt")
@file:kotlin.jvm.JvmVersion
package kotlin

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@kotlin.internal.InlineOnly
public inline fun Double.isNaN(): Boolean = java.lang.Double.isNaN(this)

/**
 * Returns `true` if the specified number is a
 * Not-a-Number (NaN) value, `false` otherwise.
 */
@kotlin.internal.InlineOnly
public inline fun Float.isNaN(): Boolean = java.lang.Float.isNaN(this)

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@kotlin.internal.InlineOnly
public inline fun Double.isInfinite(): Boolean = java.lang.Double.isInfinite(this)

/**
 * Returns `true` if this value is infinitely large in magnitude.
 */
@kotlin.internal.InlineOnly
public inline fun Float.isInfinite(): Boolean = java.lang.Float.isInfinite(this)

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@kotlin.internal.InlineOnly
public inline fun Double.isFinite(): Boolean = !isInfinite() && !isNaN()

/**
 * Returns `true` if the argument is a finite floating-point value; returns `false` otherwise (for `NaN` and infinity arguments).
 */
@kotlin.internal.InlineOnly
public inline fun Float.isFinite(): Boolean = !isInfinite() && !isNaN()


@kotlin.internal.InlineOnly
public inline fun Double.toBits(): Long = java.lang.Double.doubleToLongBits(this)
@kotlin.internal.InlineOnly
public inline fun Double.toRawBits(): Long = java.lang.Double.doubleToRawLongBits(this)
@kotlin.internal.InlineOnly
public inline fun Double.Companion.fromBits(bits: Long) = java.lang.Double.longBitsToDouble(bits)


@kotlin.internal.InlineOnly
public inline fun Float.toBits(): Int = java.lang.Float.floatToIntBits(this)
@kotlin.internal.InlineOnly
public inline fun Float.toRawBits(): Int = java.lang.Float.floatToRawIntBits(this)
@kotlin.internal.InlineOnly
public inline fun Float.Companion.fromBits(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)


