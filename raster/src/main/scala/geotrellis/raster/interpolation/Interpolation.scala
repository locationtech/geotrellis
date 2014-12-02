package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

abstract sealed class InterpolationMethod

case object NearestNeighbor extends InterpolationMethod
case object Bilinear extends InterpolationMethod
case object CubicConvolution extends InterpolationMethod
case object CubicSpline extends InterpolationMethod
case object Lanczos extends InterpolationMethod
case object Average extends InterpolationMethod
case object Mode extends InterpolationMethod


abstract class Interpolation(tile: Tile, extent: Extent) {
  protected val re = RasterExtent(tile, extent)
  protected val cols = tile.cols
  protected val rows = tile.rows

  private val westBound = extent.xmin
  private val eastBound = extent.xmax
  private val northBound = extent.ymax
  private val southBound = extent.ymin

  protected val cellwidth = re.cellwidth
  protected val cellheight = re.cellheight

  private def isValid(x: Double, y: Double) =
    x >= westBound && x <= eastBound && y >= southBound && y <= northBound

  final def interpolate(x: Double, y: Double): Int =
    if (!isValid(x, y)) NODATA
    else interpolateValid(x, y)

  final def interpolateDouble(x: Double, y: Double): Double =
    if (!isValid(x, y)) Double.NaN
    else interpolateDoubleValid(x, y)

  protected def interpolateValid(x: Double, y: Double): Int

  protected def interpolateDoubleValid(x: Double, y: Double): Double
}

object Interpolation {
  def apply(method: InterpolationMethod, tile: Tile, extent: Extent): Interpolation =
    method match {
      case NearestNeighbor => new NearestNeighborInterpolation(tile, extent)
      case Bilinear => new BilinearInterpolation(tile, extent)
      case CubicConvolution => new BicubicConvolutionInterpolation(tile, extent)
      case CubicSpline => new BicubicSplineInterpolation(tile, extent)
      case Lanczos => new LanczosInterpolation(tile, extent)
      case Average => new AverageInterpolation(tile, extent)
      case Mode => new ModeInterpolation(tile, extent)
    }
}
