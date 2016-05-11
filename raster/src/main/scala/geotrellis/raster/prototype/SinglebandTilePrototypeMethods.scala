package geotrellis.raster.prototype

import geotrellis.raster._


/**
  * Trait containing prototype methods for single-band [[Tile]]s.
  */
trait SinglebandTilePrototypeMethods extends TilePrototypeMethods[Tile] {

  /**
    * Given a [[CellType]] and numbers of columns and rows, produce a
    * new [[ArrayTile]] of the given size and the same band count as
    * the calling object.
    */
  def prototype(cellType: CellType, cols: Int, rows: Int) =
    ArrayTile.empty(cellType, cols, rows)

  /**
    * Given numbers of columns and rows, produce a new [[ArrayTile]]
    * of the given size and the same band count as the calling object.
    */
  def prototype(cols: Int, rows: Int) =
    prototype(self.cellType, cols, rows)
}
