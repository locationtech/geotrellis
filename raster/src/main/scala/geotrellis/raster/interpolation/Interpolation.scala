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
/*
case object Kriging extends InterpolationMethod
case object KrigingSimple extends InterpolationMethod
case object KrigingOrdinary extends InterpolationMethod
case object KrigingUniversal extends InterpolationMethod
case object KrigingGeo extends InterpolationMethod
*/

object InterpolationMethod {
  val DEFAULT = NearestNeighbor
}


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

  /** Checks if a point is valid. We consider it valid if it is anywhere on or within the border. */
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
      /*
       * Ideally, the Kriging module will comprise the options of the varied types of its interpolations.
       * So, only a single object will be there at the end, which will invoke the corresponding functions
       *
       * The functions parameters also have to be changed after restructuring the location of reprojection
       * code out from the Interpolation.scala
       */
      /*
      case Kriging => new KrigingInterpolation(tile, extent)
      case KrigingSimple => new KrigingSimpleInterpolation(tile, extent)
      case KrigingOrdinary => new KrigingOrdinaryInterpolation(tile, extent)
      case KrigingUniversal => new KrigingUniversalInterpolation(tile, extent)
      case KrigingGeo => new KrigingGeoInterpolation(tile, extent)
      */
    }
}
