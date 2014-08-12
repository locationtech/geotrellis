package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent

abstract sealed class InterpolationMethod

case object NearestNeighbor extends InterpolationMethod
case object Bilinear extends InterpolationMethod
case object Cubic extends InterpolationMethod
case object CubicSpline extends InterpolationMethod
case object Lanczos extends InterpolationMethod
case object Average extends InterpolationMethod
case object Mode extends InterpolationMethod


trait Interpolation {
  def interpolate(x: Double, y: Double): Int
  def interpolateDouble(x: Double, y: Double): Double
}

object Interpolation {
  def apply(method: InterpolationMethod, tile: Tile, extent: Extent): Interpolation =
    method match {
      case NearestNeighbor => new NearestNeighborInterpolation(tile, extent)
      case Bilinear => ???
      case Cubic => ???
      case CubicSpline => ???
      case Lanczos => ???
      case Average => ???
      case Mode => ???
    }
}
