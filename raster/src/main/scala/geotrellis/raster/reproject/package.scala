package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._

import spire.syntax.cfor._

package object reproject {
  // Function that takes in coordinates in the first to arrays and fills out
  // the second two arrays with transformed coordinates (srcX, srcY, dstX, dstY)
  type RowTransform = (Array[Double], Array[Double], Array[Double], Array[Double]) => Unit

  implicit class ReprojectExtentsion(val tile: Tile) {
    def reproject(extent: Extent, src: CRS, dest: CRS): (Tile, Extent) = 
      reproject(extent, src, dest, ReprojectOptions.DEFAULT)

    def reproject(method: InterpolationMethod, extent: Extent, src: CRS, dest: CRS): (Tile, Extent) = 
      reproject(extent, src, dest, ReprojectOptions(method = method))

    def reproject(extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): (Tile, Extent) =
      Reproject(tile, extent, src, dest, options)
  }
}

