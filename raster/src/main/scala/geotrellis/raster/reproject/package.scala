package geotrellis.raster

import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._

import spire.syntax.cfor._

package object reproject {
  // Function that takes in coordinates in the first to arrays and fills out
  // the second two arrays with transformed coordinates (srcX, srcY, dstX, dstY)
  type RowTransform = (Array[Double], Array[Double], Array[Double], Array[Double]) => Unit

  implicit class ReprojectExtension(val tile: Tile) {
    def reproject(extent: Extent, src: CRS, dest: CRS): Raster =
      reproject(extent, src, dest, ReprojectOptions.DEFAULT)

    def reproject(method: ResampleMethod, extent: Extent, src: CRS, dest: CRS): Raster =
      reproject(extent, src, dest, ReprojectOptions(method = method))

    def reproject(extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): Raster =
      Reproject(tile, extent, src, dest, options)
  }

  implicit class ReprojectMultiBandExtension(val tile: MultiBandTile) {
    def reproject(extent: Extent, src: CRS, dest: CRS): MultiBandRaster =
      reproject(extent, src, dest, ReprojectOptions.DEFAULT)

    def reproject(method: ResampleMethod, extent: Extent, src: CRS, dest: CRS): MultiBandRaster =
      reproject(extent, src, dest, ReprojectOptions(method = method))

    def reproject(extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): MultiBandRaster =
      Reproject(tile, extent, src, dest, options)
  }
}
