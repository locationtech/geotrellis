package geotrellis.raster.prototype

import geotrellis.raster._

trait MultibandTilePrototypeMethods extends TilePrototypeMethods[MultibandTile] {
    def prototype(cellType: CellType, cols: Int, rows: Int) =
      ArrayMultibandTile.empty(cellType, self.bandCount, cols, rows)

    def prototype(cols: Int, rows: Int) =
      prototype(self.cellType, cols, rows)
}
