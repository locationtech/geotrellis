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

import spire.math.Integral


package object raster extends Implicits with MacroFunctions {
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
    import spire.implicits.*
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
