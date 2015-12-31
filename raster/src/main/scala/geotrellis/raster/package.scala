/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis

import geotrellis.macros.{ NoDataMacros, TypeConversionMacros }

package object raster {
  type DI = DummyImplicit

  type IntTileMapper = macros.IntTileMapper
  type DoubleTileMapper = macros.DoubleTileMapper
  type IntTileVisitor = macros.IntTileVisitor
  type DoubleTileVisitor = macros.DoubleTileVisitor

  type IntTileCombiner3 = macros.IntTileCombiner3
  type DoubleTileCombiner3 = macros.DoubleTileCombiner3
  type IntTileCombiner4 = macros.IntTileCombiner4
  type DoubleTileCombiner4 = macros.DoubleTileCombiner4

  // Keep constant values in sync with macro functions
  @inline final val byteNODATA = Byte.MinValue
  @inline final val ubyteNODATA = Byte.MinValue & 0xFF
  @inline final val shortNODATA = Short.MinValue
  @inline final val ushortNODATA = Short.MinValue & 0xFFFF
  @inline final val NODATA = Int.MinValue
  @inline final val floatNODATA = Float.NaN
  @inline final val doubleNODATA = Double.NaN

  def isNoData(i: Int): Boolean = macro NoDataMacros.isNoDataInt_impl
  def isNoData(f: Float): Boolean = macro NoDataMacros.isNoDataFloat_impl
  def isNoData(d: Double): Boolean = macro NoDataMacros.isNoDataDouble_impl

  def isData(i: Int): Boolean = macro NoDataMacros.isDataInt_impl
  def isData(f: Float): Boolean = macro NoDataMacros.isDataFloat_impl
  def isData(d: Double): Boolean = macro NoDataMacros.isDataDouble_impl

  def b2i(n: Byte): Int = macro TypeConversionMacros.b2i_impl
  def b2s(n: Byte): Short = macro TypeConversionMacros.b2s_impl
  def b2f(n: Byte): Float = macro TypeConversionMacros.b2f_impl
  def b2d(n: Byte): Double = macro TypeConversionMacros.b2d_impl

  def s2b(n: Short): Byte = macro TypeConversionMacros.s2b_impl
  def s2i(n: Short): Int = macro TypeConversionMacros.s2i_impl
  def s2f(n: Short): Float = macro TypeConversionMacros.s2f_impl
  def s2d(n: Short): Double = macro TypeConversionMacros.s2d_impl

  def i2b(n: Int): Byte = macro TypeConversionMacros.i2b_impl
  def i2s(n: Int): Short = macro TypeConversionMacros.i2s_impl
  def i2f(n: Int): Float = macro TypeConversionMacros.i2f_impl
  def i2d(n: Int): Double = macro TypeConversionMacros.i2d_impl

  def f2b(n: Float): Byte = macro TypeConversionMacros.f2b_impl
  def f2s(n: Float): Short = macro TypeConversionMacros.f2s_impl
  def f2i(n: Float): Int = macro TypeConversionMacros.f2i_impl
  def f2d(n: Float): Double = macro TypeConversionMacros.f2d_impl

  def d2b(n: Double): Byte = macro TypeConversionMacros.d2b_impl
  def d2s(n: Double): Short = macro TypeConversionMacros.d2s_impl
  def d2i(n: Double): Int = macro TypeConversionMacros.d2i_impl
  def d2f(n: Double): Float = macro TypeConversionMacros.d2f_impl

  implicit class TileMethodWrapper(val tile: Tile) extends crop.TileCropMethods

  implicit class MultiBandTileMethodWrapper(val self: MultiBandTile) extends crop.MultiBandTileCropMethods

  // Use this implicit class to fill arrays ... much faster than Array.fill[Int](dim)(val), etc.
  implicit class ByteArrayFiller(val arr: Array[Byte]) extends AnyVal {
    def fill(v: Byte) = { java.util.Arrays.fill(arr, v) ; arr }
  }
  implicit class ShortArrayFiller(val arr: Array[Short]) extends AnyVal {
    def fill(v: Short) = { java.util.Arrays.fill(arr, v) ; arr }
  }
  implicit class IntArrayFiller(val arr: Array[Int]) extends AnyVal {
    def fill(v: Int) = { java.util.Arrays.fill(arr, v) ; arr }
  }
  implicit class FloatArrayFiller(val arr: Array[Float]) extends AnyVal {
    def fill(v: Float) = { java.util.Arrays.fill(arr, v) ; arr }
  }
  implicit class DoubleArrayFiller(val arr: Array[Double]) extends AnyVal {
    def fill(v: Double) = { java.util.Arrays.fill(arr, v) ; arr }
  }

  implicit class TraversableTileExtensions(rs: Traversable[Tile]) {
    def assertEqualDimensions(): Unit =
      if(Set(rs.map(_.dimensions)).size != 1) {
        val dimensions = rs.map(_.dimensions).toSeq
        throw new GeoAttrsError("Cannot combine tiles with different dimensions." +
          s"$dimensions are not all equal")
      }
  }

  implicit class TileTupleExtensions(t: (Tile, Tile)) {
    def assertEqualDimensions(): Unit =
      if(t._1.dimensions != t._2.dimensions) {
        throw new GeoAttrsError("Cannot combine rasters with different dimensions." +
          s"${t._1.dimensions} does not match ${t._2.dimensions}")
      }
  }
}
