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

    def reproject(extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): Raster = {
      Reproject(tile, extent, src, dest, options)
    }
  }

  implicit class withMultiBandTileReprojectMethods(val tile: MultiBandTile) extends ReprojectMethods[MultiBandTile] {
    type ReturnType = MultiBandRaster

    def reproject(extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): MultiBandRaster = {
      Reproject(tile, extent, src, dest, options)
    }
  }
}
