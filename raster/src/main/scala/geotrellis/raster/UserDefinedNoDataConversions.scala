package geotrellis.raster

import scala.math.ScalaNumericAnyConversions

trait UserDefinedByteNoDataConversions extends UserDefinedNoDataConversions {
  val userDefinedByteNoDataValue = userDefinedIntNoDataValue.toByte
  val userDefinedShortNoDataValue = userDefinedIntNoDataValue.toShort
  val userDefinedIntNoDataValue: Int
  val userDefinedFloatNoDataValue = userDefinedIntNoDataValue.toFloat
  val userDefinedDoubleNoDataValue = userDefinedIntNoDataValue.toDouble
}

trait UserDefinedShortNoDataConversions extends UserDefinedNoDataConversions {
  val userDefinedByteNoDataValue = userDefinedShortNoDataValue.toByte
  val userDefinedShortNoDataValue: Short
  val userDefinedIntNoDataValue = userDefinedShortNoDataValue.toInt
  val userDefinedFloatNoDataValue = userDefinedShortNoDataValue.toFloat
  val userDefinedDoubleNoDataValue = userDefinedShortNoDataValue.toDouble
}

trait UserDefinedIntNoDataConversions extends UserDefinedNoDataConversions {
  val userDefinedByteNoDataValue = userDefinedIntNoDataValue.toByte
  val userDefinedShortNoDataValue = userDefinedIntNoDataValue.toShort
  val userDefinedIntNoDataValue: Int
  val userDefinedFloatNoDataValue = userDefinedIntNoDataValue.toFloat
  val userDefinedDoubleNoDataValue = userDefinedIntNoDataValue.toDouble
}

trait UserDefinedNoDataConversions {
  val userDefinedByteNoDataValue: Byte
  val userDefinedShortNoDataValue: Short
  val userDefinedIntNoDataValue: Int
  val userDefinedFloatNoDataValue: Float
  val userDefinedDoubleNoDataValue: Double

  // user-defined to standardized space
  def udb2ub(n: Byte): Byte = if (n == userDefinedByteNoDataValue) 0.toByte else n.toByte
  def udb2s(n: Byte): Short = if (n == userDefinedByteNoDataValue) Short.MinValue else n.toShort
  def udb2us(n: Byte): Short = if (n == userDefinedByteNoDataValue) 0.toShort else n.toShort
  def udb2i(n: Byte): Int = if (n == userDefinedByteNoDataValue) Int.MinValue else n
  def udb2f(n: Byte): Float = if (n == userDefinedByteNoDataValue) Float.NaN else n.toFloat
  def udb2d(n: Byte): Double = if (n == userDefinedByteNoDataValue) Double.NaN else n.toDouble

  def uds2b(n: Short): Byte = if (n == userDefinedShortNoDataValue) Byte.MinValue else n.toByte
  def uds2ub(n: Short): Byte = if (n == userDefinedShortNoDataValue) 0.toByte else n.toByte
  def uds2us(n: Short): Short = if (n == userDefinedShortNoDataValue) 0.toShort else n.toShort
  def uds2i(n: Short): Int = if (n == userDefinedShortNoDataValue) Int.MinValue else n
  def uds2f(n: Short): Float = if (n == userDefinedShortNoDataValue) Float.NaN else n.toFloat
  def uds2d(n: Short): Double = if (n == userDefinedShortNoDataValue) Double.NaN else n.toDouble

  def udi2b(n: Int): Byte = if (n == userDefinedIntNoDataValue) Byte.MinValue else n.toByte
  def udi2ub(n: Int): Byte = if (n == userDefinedIntNoDataValue) 0.toByte else n.toByte
  def udi2s(n: Int): Short = if (n == userDefinedIntNoDataValue) Short.MinValue else n.toShort
  def udi2us(n: Int): Short = if (n == userDefinedIntNoDataValue) 0.toShort else n.toShort
  def udi2i(n: Int): Int = if (n == userDefinedIntNoDataValue) Int.MinValue else n
  def udi2f(n: Int): Float = if (n == userDefinedIntNoDataValue) Float.NaN else n.toFloat
  def udi2d(n: Int): Double = if (n == userDefinedIntNoDataValue) Double.NaN else n.toDouble

  def udf2b(n: Float): Byte = if (n == userDefinedFloatNoDataValue) Byte.MinValue else n.toByte
  def udf2ub(n: Float): Byte = if (n == userDefinedFloatNoDataValue) 0.toByte else n.toByte
  def udf2s(n: Float): Short = if (n == userDefinedFloatNoDataValue) Short.MinValue else n.toShort
  def udf2us(n: Float): Short = if (n == userDefinedFloatNoDataValue) 0.toShort else n.toShort
  def udf2i(n: Float): Int = if (n == userDefinedFloatNoDataValue) Int.MinValue else n.toInt
  def udf2d(n: Float): Double = if (n == userDefinedFloatNoDataValue) Double.NaN else n.toDouble

  def udd2b(n: Double): Byte = if (n == userDefinedFloatNoDataValue) Byte.MinValue else n.toByte
  def udd2ub(n: Double): Byte = if (n == userDefinedFloatNoDataValue) 0.toByte else n.toByte
  def udd2s(n: Double): Short = if (n == userDefinedFloatNoDataValue) Short.MinValue else n.toShort
  def udd2us(n: Double): Short = if (n == userDefinedFloatNoDataValue) 0.toShort else n.toShort
  def udd2i(n: Double): Int = if (n == userDefinedFloatNoDataValue) Int.MinValue else n.toInt
  def udd2f(n: Double): Float = if (n == userDefinedFloatNoDataValue) Float.NaN else n.toFloat

  // standardized back to user-defined space
  def b2udb(n: Byte): Byte = if (n == Byte.MinValue) userDefinedByteNoDataValue else n
  def b2uds(n: Byte): Short = if (n == Byte.MinValue) userDefinedShortNoDataValue else n.toShort
  def b2udi(n: Byte): Int = if (n == Byte.MinValue) userDefinedIntNoDataValue else n.toInt
  def b2udf(n: Byte): Float = if (n == Byte.MinValue) userDefinedFloatNoDataValue else n.toFloat
  def b2udd(n: Byte): Double = if (n == Byte.MinValue) userDefinedDoubleNoDataValue else n.toDouble

  def ub2udb(n: Byte): Byte = if (n == 0.toByte) userDefinedByteNoDataValue else n
  def ub2uds(n: Byte): Short = if (n == 0.toByte) userDefinedShortNoDataValue else n.toShort
  def ub2udi(n: Byte): Int = if (n == 0.toByte) userDefinedIntNoDataValue else n.toInt
  def ub2udf(n: Byte): Float = if (n == 0.toByte) userDefinedFloatNoDataValue else n.toFloat
  def ub2udd(n: Byte): Double = if (n == 0.toByte) userDefinedDoubleNoDataValue else n.toDouble

  def s2udb(n: Short): Byte = if (n == Short.MinValue) userDefinedByteNoDataValue else n.toByte
  def s2uds(n: Short): Short = if (n == Short.MinValue) userDefinedShortNoDataValue else n
  def s2udi(n: Short): Int = if (n == Short.MinValue) userDefinedIntNoDataValue else n.toInt
  def s2udf(n: Short): Float = if (n == Short.MinValue) userDefinedFloatNoDataValue else n.toFloat
  def s2udd(n: Short): Double = if (n == Short.MinValue) userDefinedDoubleNoDataValue else n.toDouble

  def us2udb(n: Short): Byte = if (n == 0.toShort) userDefinedByteNoDataValue else n.toByte
  def us2uds(n: Short): Short = if (n == 0.toShort) userDefinedShortNoDataValue else n
  def us2udi(n: Short): Int = if (n == 0.toShort) userDefinedIntNoDataValue else n.toInt
  def us2udf(n: Short): Float = if (n == 0.toShort) userDefinedFloatNoDataValue else n.toFloat
  def us2udd(n: Short): Double = if (n == 0.toShort) userDefinedDoubleNoDataValue else n.toDouble

  def i2udb(n: Int): Byte = if (n == Int.MinValue) userDefinedByteNoDataValue else n.toByte
  def i2uds(n: Int): Short = if (n == Int.MinValue) userDefinedShortNoDataValue else n.toShort
  def i2udi(n: Int): Int = if (n == Int.MinValue) userDefinedIntNoDataValue else n
  def i2udf(n: Int): Float = if (n == Int.MinValue) userDefinedFloatNoDataValue else n.toFloat
  def i2udd(n: Int): Double = if (n == Int.MinValue) userDefinedDoubleNoDataValue else n.toDouble

  def f2udb(n: Float): Byte = if (n == Float.NaN) userDefinedByteNoDataValue else n.toByte
  def f2uds(n: Float): Short = if (n == Float.NaN) userDefinedShortNoDataValue else n.toShort
  def f2udi(n: Float): Int = if (n == Float.NaN) userDefinedIntNoDataValue else n.toInt
  def f2udf(n: Float): Float = if (n == Float.NaN) userDefinedFloatNoDataValue else n
  def f2udd(n: Float): Double = if (n == Float.NaN) userDefinedDoubleNoDataValue else n.toDouble

  def d2udb(n: Double): Byte = if (n == Double.NaN) userDefinedByteNoDataValue else n.toByte
  def d2uds(n: Double): Short = if (n == Double.NaN) userDefinedShortNoDataValue else n.toShort
  def d2udi(n: Double): Int = if (n == Double.NaN) userDefinedIntNoDataValue else n.toInt
  def d2udf(n: Double): Float = if (n == Double.NaN) userDefinedFloatNoDataValue else n.toFloat
  def d2udd(n: Double): Double = if (n == Double.NaN) userDefinedDoubleNoDataValue else n
}
