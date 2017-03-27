package geotrellis.raster.transform

import geotrellis.raster.{CellGrid, MultibandTile, Raster, Tile}

object Implicits extends Implicits

trait Implicits {
  implicit class withTransformTileMethods(val self: Tile) extends TransformTileMethods

  implicit class withTransformMultibandTileMethods(val self: MultibandTile) extends TransformMethods[MultibandTile] {
    def rotate90(n: Int = 1): MultibandTile = self.mapBands { (_, tile) => tile.rotate90(n) }
    def flipVertical: MultibandTile = self.mapBands { (_, tile) => tile.flipVertical }
    def flipHorizontal: MultibandTile = self.mapBands { (_, tile) => tile.flipHorizontal }
    def zscore: MultibandTile = self.mapBands { (_, tile) => tile.zscore }
  }

  implicit class withTransformRasterMethods[T <: CellGrid: ? => TransformMethods[T]](val self: Raster[T]) extends TransformMethods[Raster[T]] {
    def rotate90(n: Int = 1): Raster[T] = Raster(self.tile.rotate90(n), self.extent)
    def flipVertical: Raster[T] = Raster(self.tile.flipVertical, self.extent)
    def flipHorizontal: Raster[T] = Raster(self.tile.flipHorizontal, self.extent)
    def zscore: Raster[T] = Raster(self.tile.zscore, self.extent)
  }
}
