package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.proj4._

package object reproject {
  // Function that takes in coordinates in the first to arrays and fills out
  // the second two arrays with transformed coordinates (srcX, srcY, dstX, dstY)
  type RowTransform = (Array[Double], Array[Double], Array[Double], Array[Double]) => Unit

  type ReprojectView[TileType] = TileType => ReprojectMethods[TileType]

  implicit class withTileReprojectMethods(val tile: Tile) extends ReprojectMethods[Tile] {
    type ReturnType = Raster

    def reproject(extent: Extent, src: CRS, dest: CRS): SingleBandReproject.Apply = {
      SingleBandReproject(tile, extent, src, dest)
    }
  }

  implicit class withMultiBandTileReprojectMethods(val tile: MultiBandTile) extends ReprojectMethods[MultiBandTile] {
    type ReturnType = MultiBandRaster

    def reproject(extent: Extent, src: CRS, dest: CRS): MultiBandReproject.Apply = {
      MultiBandReproject(tile, extent, src, dest)
    }
  }

  implicit class withRasterReprojectMethods(val raster: Raster) {
    def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): SingleBandReproject.Apply =
      SingleBandReproject(raster, targetRasterExtent, transform, inverseTransform)

    def reproject(src: CRS, dest: CRS): SingleBandReproject.Apply =
      SingleBandReproject(raster, src, dest)

    def reproject(window: GridBounds, src: CRS, dest: CRS): SingleBandReproject.Apply =
      SingleBandReproject(window, raster, src, dest)

    def reproject(window: GridBounds, transform: Transform, inverseTransform: Transform): SingleBandReproject.Apply =
      SingleBandReproject(window, raster, transform, inverseTransform)
  }

  implicit class withMultiBandRasterReprojectMethods(val raster: MultiBandRaster) {
    def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): MultiBandReproject.Apply =
      MultiBandReproject(raster, targetRasterExtent, transform, inverseTransform)

    def reproject(window: GridBounds, src: CRS, dest: CRS): MultiBandReproject.Apply =
      MultiBandReproject(window, raster, src, dest)

    def reproject(window: GridBounds, transform: Transform, inverseTransform: Transform): MultiBandReproject.Apply =
      MultiBandReproject(window, raster, transform, inverseTransform)
  }
}
