package geotrellis.raster.prototype

import geotrellis.raster._

trait SinglebandTilePrototypeMethods extends TilePrototypeMethods[Tile] {
  def prototype(cellType: CellType, cols: Int, rows: Int) =
    ArrayTile.empty(cellType, cols, rows)

  def prototype(cols: Int, rows: Int) =
    prototype(self.cellType, cols, rows)
}
