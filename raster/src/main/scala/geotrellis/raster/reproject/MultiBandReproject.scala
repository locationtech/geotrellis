package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

object MultiBandReproject {
  type Apply = Reproject.Apply[MultiBandRaster]

  class CallApply(
    raster: MultiBandRaster,
    targetRasterExtent: RasterExtent,
    transform: Transform,
    inverseTransform: Transform
  ) extends Apply {
    def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): MultiBandRaster = {
      val MultiBandRaster(tile, extent) = raster
      val bands =
        for ( bandIndex <- 0 until tile.bandCount ) yield {
          Raster(tile.band(bandIndex), raster.extent).reproject(targetRasterExtent, transform, inverseTransform)(method = method, errorThreshold = errorThreshold)
        }

      MultiBandRaster(ArrayMultiBandTile(bands.map(_.tile)), bands.head.extent)
    }
  }
  implicit def applyToCall(a: Apply): MultiBandRaster = a()

  def apply(tile: MultiBandTile, extent: Extent, src: CRS, dest: CRS): Apply = {
    val raster = MultiBandRaster(tile, extent)
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    val targetRasterExtent = ReprojectRasterExtent(raster.rasterExtent, transform)

    apply(raster, targetRasterExtent, transform, inverseTransform)
  }

  def apply(
    raster: MultiBandRaster,
    targetRasterExtent: RasterExtent,
    transform: Transform,
    inverseTransform: Transform
  ): Apply =
    new CallApply(raster, targetRasterExtent, transform, inverseTransform)

  def apply(gridBounds: GridBounds, tile: MultiBandTile, extent: Extent, src: CRS, dest: CRS): Apply =
    apply(gridBounds, MultiBandRaster(tile, extent), src, dest)

  def apply(gridBounds: GridBounds, raster: MultiBandRaster, src: CRS, dest: CRS): Apply = {
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    apply(gridBounds, raster, transform, inverseTransform)
  }

  def apply(gridBounds: GridBounds, raster: MultiBandRaster, transform: Transform, inverseTransform: Transform): Apply = {
    val rasterExtent = raster.rasterExtent
    val windowExtent = rasterExtent.extentFor(gridBounds)
    val windowRasterExtent = RasterExtent(windowExtent, gridBounds.width, gridBounds.height)
    val targetRasterExtent = ReprojectRasterExtent(windowRasterExtent, transform)

    apply(raster, targetRasterExtent, transform, inverseTransform)
  }

}
