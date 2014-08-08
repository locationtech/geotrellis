package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._

import spire.syntax.cfor._

package object reproject {
  // Function that takes in coordinates in the first to arrays and fills out
  // the second two arrays with transformed coordinates (srcX, srcY, dstX, dstY)
  type RowTransform = (Array[Double], Array[Double], Array[Double], Array[Double]) => Unit

  abstract sealed class InterpolationMethod
  case object NearestNeighbor extends InterpolationMethod
  case object Bilinear extends InterpolationMethod
  case object Cubic extends InterpolationMethod
  case object CubicSpline extends InterpolationMethod
  case object Lanczos extends InterpolationMethod
  case object AverageInterpolation extends InterpolationMethod
  case object ModeInterpolation extends InterpolationMethod

  case class ReprojectOptions(method: InterpolationMethod = Bilinear, errorThreshold: Double = 0.125)
  object ReprojectOptions { 
    val DEFAULT = ReprojectOptions()
  }

  implicit val defaultReprojectOptions = ReprojectOptions.DEFAULT

  implicit class ReprojectExtentsion(val tile: Tile) {
    def reproject(extent: Extent, src: CRS, dest: CRS)(implicit options: ReprojectOptions): (Tile, Extent) = 
      Reproject(tile, extent, src, dest)(options)
  }
}

