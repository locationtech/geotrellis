package geotrellis.raster.prototype

import geotrellis.raster._

trait MultiBandTilePrototypeMethods extends TilePrototypeMethods[MultiBandTile] {
    def prototype(cellType: CellType, cols: Int, rows: Int) =
      ArrayMultiBandTile.empty(cellType, self.bandCount, cols, rows)

    def prototype(cols: Int, rows: Int) =
      prototype(self.cellType, cols, rows)
}
