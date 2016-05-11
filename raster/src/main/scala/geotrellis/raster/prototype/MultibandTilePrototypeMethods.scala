package geotrellis.raster.prototype

import geotrellis.raster._


/**
  * Trait containing prototype methods for [[MultibandTile]]s.
  */
trait MultibandTilePrototypeMethods extends TilePrototypeMethods[MultibandTile] {

  /**
    * Given a [[CellType]] and numbers of columns and rows, produce a
    * new [[ArrayMultibandTile]] of the given size and the same band
    * count as the calling object.
    */
  def prototype(cellType: CellType, cols: Int, rows: Int) =
    ArrayMultibandTile.empty(cellType, self.bandCount, cols, rows)

  /**
    * Given numbers of columns and rows, produce a new
    * [[ArrayMultibandTile]] of the given size and the same band count
    * as the calling object.
    */
  def prototype(cols: Int, rows: Int) =
    prototype(self.cellType, cols, rows)
}
