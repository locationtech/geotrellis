package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

trait MultiBandRasterReprojectMethods extends RasterReprojectMethods[MultiBandRaster] {
  import Reproject.Options

  def reproject(
    targetRasterExtent: RasterExtent,
    transform: Transform,
    inverseTransform: Transform,
    options: Options
  ): MultiBandRaster = {
    val Raster(tile, extent) = self
    val bands =
      for(bandIndex <- 0 until tile.bandCount ) yield {
        Raster(tile.band(bandIndex), self.extent)
          .reproject(targetRasterExtent, transform, inverseTransform, options)
      }

    Raster(ArrayMultiBandTile(bands.map(_.tile)), bands.head.extent)
  }
}
