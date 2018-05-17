/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

trait UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue: Byte

  // from user defined
  def udb2b(n: Byte): Byte = if (n == userDefinedByteNoDataValue) Byte.MinValue else n
  def udb2ub(n: Byte): Byte = if (n == userDefinedByteNoDataValue) 0.toByte else n
  def udb2s(n: Byte): Short = if (n == userDefinedByteNoDataValue) Short.MinValue else n.toShort
  def udb2us(n: Byte): Short = if (n == userDefinedByteNoDataValue) 0.toShort else (n & 0xFF).toShort
  def udb2i(n: Byte): Int = if (n == userDefinedByteNoDataValue) Int.MinValue else n.toInt
  def udub2i(n: Byte): Int = if (n == userDefinedByteNoDataValue) Int.MinValue else n & 0xFF
  def udb2f(n: Byte): Float = if (n == userDefinedByteNoDataValue) Float.NaN else n.toFloat
  def udub2f(n: Byte): Float = if (n == userDefinedByteNoDataValue) Float.NaN else (n & 0xFF).toFloat
  def udb2d(n: Byte): Double = if (n == userDefinedByteNoDataValue) Double.NaN else n.toDouble
  def udub2d(n: Byte): Double = if (n == userDefinedByteNoDataValue) Double.NaN else (n & 0xFF).toDouble

  // to user defined
  def b2udb(n: Byte): Byte = if (n == Byte.MinValue) userDefinedByteNoDataValue else n
  def ub2udb(n: Byte): Byte = if (n == 0.toByte) userDefinedByteNoDataValue else n
  def s2udb(n: Short): Byte = if (n == Short.MinValue) userDefinedByteNoDataValue else n.toByte
  def us2udb(n: Short): Byte = if (n == 0.toShort) userDefinedByteNoDataValue else n.toByte
  def i2udb(n: Int): Byte = if (n == Int.MinValue) userDefinedByteNoDataValue else n.toByte
  def f2udb(n: Float): Byte = if (isNoData(n)) userDefinedByteNoDataValue else n.toByte
  def d2udb(n: Double): Byte = if (isNoData(n)) userDefinedByteNoDataValue else n.toByte
}

trait UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue: Short

  // from user defined
  def uds2b(n: Short): Byte = if (n == userDefinedShortNoDataValue) Byte.MinValue else n.toByte
  def uds2ub(n: Short): Byte = if (n == userDefinedShortNoDataValue) 0.toByte else n.toByte
  def uds2s(n: Short): Short = if (n == userDefinedShortNoDataValue) Short.MinValue else n
  def uds2us(n: Short): Short = if (n == userDefinedShortNoDataValue) 0.toShort else n
  def uds2i(n: Short): Int = if (n == userDefinedShortNoDataValue) Int.MinValue else n.toInt
  def udus2i(n: Short): Int = if (n == userDefinedShortNoDataValue) Int.MinValue else n & 0xFFFF
  def uds2f(n: Short): Float = if (n == userDefinedShortNoDataValue) Float.NaN else n.toFloat
  def udus2f(n: Short): Float = if (n == userDefinedShortNoDataValue) Float.NaN else (n & 0xFFFF).toFloat
  def uds2d(n: Short): Double = if (n == userDefinedShortNoDataValue) Double.NaN else n.toDouble
  def udus2d(n: Short): Double = if (n == userDefinedShortNoDataValue) Double.NaN else (n & 0xFFFF).toDouble
  // to user defined
  def b2uds(n: Byte): Short = if (n == Byte.MinValue) userDefinedShortNoDataValue else n.toShort
  def ub2uds(n: Byte): Short = if (n == 0.toByte) userDefinedShortNoDataValue else n.toShort
  def s2uds(n: Short): Short = if (n == Short.MinValue) userDefinedShortNoDataValue else n
  def us2uds(n: Short): Short = if (n == 0.toShort) userDefinedShortNoDataValue else n
  def i2uds(n: Int): Short = if (n == Int.MinValue) userDefinedShortNoDataValue else n.toShort
  def f2uds(n: Float): Short = if (isNoData(n)) userDefinedShortNoDataValue else n.toShort
  def d2uds(n: Double): Short = if (isNoData(n)) userDefinedShortNoDataValue else n.toShort
}

trait UserDefinedIntNoDataConversions {
  val userDefinedIntNoDataValue: Int

  // from user defined
  def udi2b(n: Int): Byte = if (n == userDefinedIntNoDataValue) Byte.MinValue else n.toByte
  def udi2ub(n: Int): Byte = if (n == userDefinedIntNoDataValue) 0.toByte else n.toByte
  def udi2s(n: Int): Short = if (n == userDefinedIntNoDataValue) Short.MinValue else n.toShort
  def udi2us(n: Int): Short = if (n == userDefinedIntNoDataValue) 0.toShort else n.toShort
  def udi2i(n: Int): Int = if (n == userDefinedIntNoDataValue) Int.MinValue else n
  def udi2f(n: Int): Float = if (n == userDefinedIntNoDataValue) Float.NaN else n.toFloat
  def udi2d(n: Int): Double = if (n == userDefinedIntNoDataValue) Double.NaN else n.toDouble
  // to user defined
  def b2udi(n: Byte): Int = if (n == Byte.MinValue) userDefinedIntNoDataValue else n.toInt
  def ub2udi(n: Byte): Int = if (n == 0.toByte) userDefinedIntNoDataValue else n.toInt
  def s2udi(n: Short): Int = if (n == Short.MinValue) userDefinedIntNoDataValue else n.toInt
  def us2udi(n: Short): Int = if (n == 0.toShort) userDefinedIntNoDataValue else n.toInt
  def i2udi(n: Int): Int = if (n == Int.MinValue) userDefinedIntNoDataValue else n
  def f2udi(n: Float): Int = if (isNoData(n)) userDefinedIntNoDataValue else n.toInt
  def d2udi(n: Double): Int = if (isNoData(n)) userDefinedIntNoDataValue else n.toInt
}

trait UserDefinedFloatNoDataConversions {
  val userDefinedFloatNoDataValue: Float

  // from user defined
  def udf2b(n: Float): Byte = if (n == userDefinedFloatNoDataValue) Byte.MinValue else n.toByte
  def udf2ub(n: Float): Byte = if (n == userDefinedFloatNoDataValue) 0.toByte else n.toByte
  def udf2s(n: Float): Short = if (n == userDefinedFloatNoDataValue) Short.MinValue else n.toShort
  def udf2us(n: Float): Short = if (n == userDefinedFloatNoDataValue) 0.toShort else n.toShort
  def udf2i(n: Float): Int = if (n == userDefinedFloatNoDataValue) Int.MinValue else n.toInt
  def udf2f(n: Float): Float = if (n == userDefinedFloatNoDataValue) Float.NaN else n.toFloat
  def udf2d(n: Float): Double = if (n == userDefinedFloatNoDataValue) Double.NaN else n.toDouble
  // to user defined
  def b2udf(n: Byte): Float = if (n == Byte.MinValue) userDefinedFloatNoDataValue else n.toFloat
  def ub2udf(n: Byte): Float = if (n == 0.toByte) userDefinedFloatNoDataValue else n.toFloat
  def s2udf(n: Short): Float = if (n == Short.MinValue) userDefinedFloatNoDataValue else n.toFloat
  def us2udf(n: Short): Float = if (n == 0.toShort) userDefinedFloatNoDataValue else n.toFloat
  def i2udf(n: Int): Float = if (n == Int.MinValue) userDefinedFloatNoDataValue else n.toFloat
  def f2udf(n: Float): Float = if (isNoData(n)) userDefinedFloatNoDataValue else n
  def d2udf(n: Double): Float = if (isNoData(n)) userDefinedFloatNoDataValue else n.toFloat
}

trait UserDefinedDoubleNoDataConversions {
  val userDefinedDoubleNoDataValue: Double

  // from user defined
  def udd2b(n: Double): Byte = if (n == userDefinedDoubleNoDataValue) Byte.MinValue else n.toByte
  def udd2ub(n: Double): Byte = if (n == userDefinedDoubleNoDataValue) 0.toByte else n.toByte
  def udd2s(n: Double): Short = if (n == userDefinedDoubleNoDataValue) Short.MinValue else n.toShort
  def udd2us(n: Double): Short = if (n == userDefinedDoubleNoDataValue) 0.toShort else n.toShort
  def udd2i(n: Double): Int = if (n == userDefinedDoubleNoDataValue) Int.MinValue else n.toInt
  def udd2f(n: Double): Float = if (n == userDefinedDoubleNoDataValue) Float.NaN else n.toFloat
  def udd2d(n: Double): Double = if (n == userDefinedDoubleNoDataValue) Double.NaN else n.toDouble
  // to user defined
  def b2udd(n: Byte): Double = if (n == Byte.MinValue) userDefinedDoubleNoDataValue else n.toDouble
  def ub2udd(n: Byte): Double = if (n == 0.toByte) userDefinedDoubleNoDataValue else n.toDouble
  def s2udd(n: Short): Double = if (n == Short.MinValue) userDefinedDoubleNoDataValue else n.toDouble
  def us2udd(n: Short): Double = if (n == 0.toShort) userDefinedDoubleNoDataValue else n.toDouble
  def i2udd(n: Int): Double = if (n == Int.MinValue) userDefinedDoubleNoDataValue else n.toDouble
  def f2udd(n: Float): Double = if (isNoData(n)) userDefinedDoubleNoDataValue else n.toDouble
  def d2udd(n: Double): Double = if (isNoData(n)) userDefinedDoubleNoDataValue else n
}
