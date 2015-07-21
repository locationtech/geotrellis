package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

abstract sealed class ResampleMethod

case object NearestNeighbor extends ResampleMethod
case object Bilinear extends ResampleMethod
case object CubicConvolution extends ResampleMethod
case object CubicSpline extends ResampleMethod
case object Lanczos extends ResampleMethod
case object Average extends ResampleMethod
case object Mode extends ResampleMethod

object ResampleMethod {
  val DEFAULT = NearestNeighbor
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
  def apply(method: ResampleMethod, tile: Tile, extent: Extent): Resample =
    method match {
      case NearestNeighbor => new NearestNeighborResample(tile, extent)
      case Bilinear => new BilinearResample(tile, extent)
      case CubicConvolution => new BicubicConvolutionResample(tile, extent)
      case CubicSpline => new BicubicSplineResample(tile, extent)
      case Lanczos => new LanczosResample(tile, extent)
      case Average => new AverageResample(tile, extent)
      case Mode => new ModeResample(tile, extent)
    }
}
