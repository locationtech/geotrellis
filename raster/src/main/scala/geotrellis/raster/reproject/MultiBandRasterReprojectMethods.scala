package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

trait MultiBandRasterReprojectMethods extends ReprojectMethods[MultiBandRaster] {
  import Reproject.Options

  def reproject(
    targetRasterExtent: RasterExtent,
    transform: Transform,
    inverseTransform: Transform,
    options: Options
  ): MultiBandRaster = {
    val MultiBandRaster(tile, extent) = self
    val bands =
      for ( bandIndex <- 0 until tile.bandCount ) yield {
        Raster(tile.band(bandIndex), self.extent)
          .reproject(targetRasterExtent, transform, inverseTransform, options)
      }

    MultiBandRaster(ArrayMultiBandTile(bands.map(_.tile)), bands.head.extent)
  }

  def reproject(
    targetRasterExtent: RasterExtent,
    transform: Transform,
    inverseTransform: Transform
  ): MultiBandRaster =
    reproject(targetRasterExtent, transform, inverseTransform, Options.DEFAULT)

  def reproject(src: CRS, dest: CRS, options: Options): MultiBandRaster = {
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    val targetRasterExtent = ReprojectRasterExtent(self.rasterExtent, transform)

    reproject(targetRasterExtent, transform, inverseTransform, options)
  }

  def reproject(src: CRS, dest: CRS): MultiBandRaster =
    reproject(src, dest, Options.DEFAULT)

  // Windowed

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): MultiBandRaster = {
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    reproject(gridBounds, transform, inverseTransform, options)
  }

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS): MultiBandRaster =
    reproject(gridBounds, src, dest, Options.DEFAULT)

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform, options: Options): MultiBandRaster = {
    val rasterExtent = self.rasterExtent
    val windowExtent = rasterExtent.extentFor(gridBounds)
    val windowRasterExtent = RasterExtent(windowExtent, gridBounds.width, gridBounds.height)
    val targetRasterExtent = ReprojectRasterExtent(windowRasterExtent, transform)

    reproject(targetRasterExtent, transform, inverseTransform, options)
  }

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform): MultiBandRaster =
    reproject(gridBounds, transform, inverseTransform, Options.DEFAULT)

}
