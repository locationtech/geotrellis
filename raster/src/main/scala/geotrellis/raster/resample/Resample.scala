package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

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

  final def resample(x: Double, y: Double): Int =
    if (!isValid(x, y)) NODATA
    else resampleValid(x, y)

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
    }
}
