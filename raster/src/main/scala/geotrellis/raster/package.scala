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

package geotrellis

import geotrellis.macros.{NoDataMacros, TypeConversionMacros}
import spire.math.Integral


package object raster extends Implicits {
  type CellType = DataType with NoDataHandling
  type SinglebandRaster = Raster[Tile]
  type MultibandRaster = Raster[MultibandTile]

  type DI = DummyImplicit

  type IntTileMapper = macros.IntTileMapper
  type DoubleTileMapper = macros.DoubleTileMapper
  type IntTileVisitor = macros.IntTileVisitor
  type DoubleTileVisitor = macros.DoubleTileVisitor

  val CropOptions = crop.Crop.Options
  val RasterizerOptions = rasterize.Rasterizer.Options
  val ColorMapOptions = render.ColorMap.Options
  val SplitOptions = split.Split.Options

  val IntHistogram = histogram.IntHistogram
  val DoubleHistogram = histogram.DoubleHistogram

  type FastMapHistogram = histogram.FastMapHistogram
  val FastMapHistogram = histogram.FastMapHistogram

  // Is @specialized required here?
  type Histogram[@specialized (Int, Double) T <: AnyVal] = histogram.Histogram[T]

  type MutableHistogram[@specialized (Int, Double) T <: AnyVal] = histogram.MutableHistogram[T]

  type StreamingHistogram = histogram.StreamingHistogram
  val StreamingHistogram = histogram.StreamingHistogram

  val CellValue = rasterize.CellValue

  type ColorMap = render.ColorMap
  val ColorMap = render.ColorMap
  val ColorMaps = render.ColorMaps

  val ColorRamp = render.ColorRamp
  val ColorRamps = render.ColorRamps

  val JpgSettings = render.jpg.Settings
  val PngSettings = render.png.Settings
  val PngColorEncoding = render.png.PngColorEncoding

  val RGB = render.RGB
  val RGBA = render.RGBA

  type ResampleMethod = resample.ResampleMethod

  object ResampleMethods {
    val NearestNeighbor = resample.NearestNeighbor
    val Bilinear = resample.Bilinear
    val CubicConvolution = resample.CubicConvolution
    val CubcSpline = resample.CubicSpline
    val Lanczos = resample.Lanczos

    val Average = resample.Average
    val Mode = resample.Mode
    val Median = resample.Median
    val Max = resample.Max
    val Min = resample.Min
    val Sum = resample.Sum
  }

  type Neighborhood = mapalgebra.focal.Neighborhood

  type TargetCell = mapalgebra.focal.TargetCell
  val TargetCell = mapalgebra.focal.TargetCell

  object Neighborhoods {
    val Square = mapalgebra.focal.Square
    val Circle = mapalgebra.focal.Circle
    val Nesw = mapalgebra.focal.Nesw
    val Wedge = mapalgebra.focal.Wedge
    val Annulus = mapalgebra.focal.Annulus
  }

  val Stitcher = stitch.Stitcher

  val ZFactor = mapalgebra.focal.ZFactor

  // Keep constant values in sync with macro functions
  @inline final val byteNODATA = Byte.MinValue
  @inline final val ubyteNODATA = 0.toByte
  @inline final val shortNODATA = Short.MinValue
  @inline final val ushortNODATA = 0.toShort
  @inline final val NODATA = Int.MinValue
  @inline final val floatNODATA = Float.NaN
  @inline final val doubleNODATA = Double.NaN

  def isNoData(i: Int): Boolean = macro NoDataMacros.isNoDataInt_impl
  def isNoData(f: Float): Boolean = macro NoDataMacros.isNoDataFloat_impl
  def isNoData(d: Double): Boolean = macro NoDataMacros.isNoDataDouble_impl

  def isData(i: Int): Boolean = macro NoDataMacros.isDataInt_impl
  def isData(f: Float): Boolean = macro NoDataMacros.isDataFloat_impl
  def isData(d: Double): Boolean = macro NoDataMacros.isDataDouble_impl

  def b2ub(n: Byte): Byte = macro TypeConversionMacros.b2ub_impl
  def b2s(n: Byte): Short = macro TypeConversionMacros.b2s_impl
  def b2us(n: Byte): Short = macro TypeConversionMacros.b2us_impl
  def b2i(n: Byte): Int = macro TypeConversionMacros.b2i_impl
  def b2f(n: Byte): Float = macro TypeConversionMacros.b2f_impl
  def b2d(n: Byte): Double = macro TypeConversionMacros.b2d_impl

  def ub2b(n: Byte): Byte = macro TypeConversionMacros.ub2b_impl
  def ub2s(n: Byte): Short = macro TypeConversionMacros.ub2s_impl
  def ub2us(n: Byte): Short = macro TypeConversionMacros.ub2us_impl
  def ub2i(n: Byte): Int = macro TypeConversionMacros.ub2i_impl
  def ub2f(n: Byte): Float = macro TypeConversionMacros.ub2f_impl
  def ub2d(n: Byte): Double = macro TypeConversionMacros.ub2d_impl

  def s2b(n: Short): Byte = macro TypeConversionMacros.s2b_impl
  def s2ub(n: Short): Byte = macro TypeConversionMacros.s2ub_impl
  def s2us(n: Short): Short = macro TypeConversionMacros.s2us_impl
  def s2i(n: Short): Int = macro TypeConversionMacros.s2i_impl
  def s2f(n: Short): Float = macro TypeConversionMacros.s2f_impl
  def s2d(n: Short): Double = macro TypeConversionMacros.s2d_impl

  def us2b(n: Short): Byte = macro TypeConversionMacros.us2b_impl
  def us2ub(n: Short): Byte = macro TypeConversionMacros.us2ub_impl
  def us2s(n: Short): Short = macro TypeConversionMacros.us2s_impl
  def us2i(n: Short): Int = macro TypeConversionMacros.us2i_impl
  def us2f(n: Short): Float = macro TypeConversionMacros.us2f_impl
  def us2d(n: Short): Double = macro TypeConversionMacros.us2d_impl

  def i2b(n: Int): Byte = macro TypeConversionMacros.i2b_impl
  def i2ub(n: Int): Byte = macro TypeConversionMacros.i2ub_impl
  def i2s(n: Int): Short = macro TypeConversionMacros.i2s_impl
  def i2us(n: Int): Short = macro TypeConversionMacros.i2us_impl
  def i2f(n: Int): Float = macro TypeConversionMacros.i2f_impl
  def i2d(n: Int): Double = macro TypeConversionMacros.i2d_impl

  def f2b(n: Float): Byte = macro TypeConversionMacros.f2b_impl
  def f2ub(n: Float): Byte = macro TypeConversionMacros.f2ub_impl
  def f2s(n: Float): Short = macro TypeConversionMacros.f2s_impl
  def f2us(n: Float): Short = macro TypeConversionMacros.f2us_impl
  def f2i(n: Float): Int = macro TypeConversionMacros.f2i_impl
  def f2d(n: Float): Double = macro TypeConversionMacros.f2d_impl

  def d2b(n: Double): Byte = macro TypeConversionMacros.d2b_impl
  def d2ub(n: Double): Byte = macro TypeConversionMacros.d2ub_impl
  def d2s(n: Double): Short = macro TypeConversionMacros.d2s_impl
  def d2us(n: Double): Short = macro TypeConversionMacros.d2us_impl
  def d2i(n: Double): Int = macro TypeConversionMacros.d2i_impl
  def d2f(n: Double): Float = macro TypeConversionMacros.d2f_impl

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


  /* http://stackoverflow.com/questions/3508077/how-to-define-type-disjunction-union-types */
  sealed class TileOrMultibandTile[T]
  object TileOrMultibandTile {
    implicit object TileWitness extends TileOrMultibandTile[Tile]
    implicit object MultibandTileWitness extends TileOrMultibandTile[MultibandTile]
  }

  private[raster] def integralIterator[@specialized(Int, Long) N: Integral](start: N, end: N, step: N): Iterator[N] = new Iterator[N] {
    import spire.implicits._
    require(start < end, s"start: $start >= end: $end")
    private var nextValue = start
    def hasNext: Boolean = nextValue < end
    def next(): N = {
      val ret = nextValue
      nextValue = nextValue + step
      ret
    }
  }
}
