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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.{Extent, Point}

sealed trait ResampleMethod

sealed trait PointResampleMethod extends ResampleMethod
case object NearestNeighbor  extends PointResampleMethod
case object Bilinear         extends PointResampleMethod
case object CubicConvolution extends PointResampleMethod
case object CubicSpline      extends PointResampleMethod
case object Lanczos          extends PointResampleMethod

sealed trait AggregateResampleMethod extends ResampleMethod
case object Average extends AggregateResampleMethod
case object Mode    extends AggregateResampleMethod
case object Median    extends AggregateResampleMethod
case object Max    extends AggregateResampleMethod
case object Min    extends AggregateResampleMethod
case object Sum extends AggregateResampleMethod

object ResampleMethod {
  val DEFAULT: PointResampleMethod = NearestNeighbor
}

abstract class Resample(tile: Tile, extent: Extent) {
  protected val re = RasterExtent(tile, extent)
  protected val cols = tile.cols
  protected val rows = tile.rows

  private val westBound = extent.xmin
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  protected val cellwidth = re.cellwidth
  protected val cellheight = re.cellheight

  /** Checks if a point is valid. We consider it valid if it is anywhere on or within the border. */
  private def isValid(x: Double, y: Double) =
    x >= westBound && x <= eastBound && y >= southBound && y <= northBound

  final def resample(p: Point): Int =
    resample(p.x, p.y)

  final def resample(x: Double, y: Double): Int =
    if (!isValid(x, y)) NODATA
    else resampleValid(x, y)

  final def resampleDouble(p: Point): Double =
    resampleDouble(p.x, p.y)

  final def resampleDouble(x: Double, y: Double): Double =
    if (!isValid(x, y)) Double.NaN
    else resampleDoubleValid(x, y)

  protected def resampleValid(x: Double, y: Double): Int

  protected def resampleDoubleValid(x: Double, y: Double): Double
}

object Resample {
  /** Create a resampler.
    *
    * @param      method         The method [[ResampleMethod]] to use.
    * @param      tile           The tile that is the source of the resample
    * @param      extent         The extent of source tile.
    */
  def apply(method: PointResampleMethod, tile: Tile, extent: Extent): Resample =
    method match {
      case NearestNeighbor => new NearestNeighborResample(tile, extent)
      case Bilinear => new BilinearResample(tile, extent)
      case CubicConvolution => new BicubicConvolutionResample(tile, extent)
      case CubicSpline => new BicubicSplineResample(tile, extent)
      case Lanczos => new LanczosResample(tile, extent)
    }

  /** Create a resampler.
    *
    * @param      method         The method [[ResampleMethod]] to use.
    * @param      tile           The tile that is the source of the resample
    * @param      extent         The extent of source tile.
    * @param      cs             The cell size of the target, for usage with Aggregate resample methods.
    */
  def apply(method: ResampleMethod, tile: Tile, extent: Extent, cs: CellSize): Resample =
    method match {
      case NearestNeighbor => new NearestNeighborResample(tile, extent)
      case Bilinear => new BilinearResample(tile, extent)
      case CubicConvolution => new BicubicConvolutionResample(tile, extent)
      case CubicSpline => new BicubicSplineResample(tile, extent)
      case Lanczos => new LanczosResample(tile, extent)
      case Average => new AverageResample(tile, extent, cs)
      case Mode => new ModeResample(tile, extent, cs)
      case Median => new MedianResample(tile, extent, cs)
      case Max => new MaxResample(tile, extent, cs)
      case Min => new MinResample(tile, extent, cs)
      case Sum =>  new SumResample(tile, extent, cs)
    }
}
