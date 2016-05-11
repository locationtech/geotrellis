package geotrellis.raster.split

import geotrellis.raster._

import spire.syntax.cfor._

import Split.Options

trait SinglebandTileSplitMethods extends SplitMethods[Tile] {
  def split(tileLayout: TileLayout, options: Options): Array[Tile] = {
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    val tiles = Array.ofDim[Tile](tileLayout.layoutCols * tileLayout.layoutRows)
    cfor(0)(_ < tileLayout.layoutRows, _ + 1) { layoutRow =>
      cfor(0)(_ < tileLayout.layoutCols, _ + 1) { layoutCol =>
        val firstCol = layoutCol * tileCols
        val lastCol = {
          val x = firstCol + tileCols - 1
          if(!options.extend && x > self.cols - 1) self.cols - 1
          else x
        }
        val firstRow = layoutRow * tileRows
        val lastRow = {
          val x = firstRow + tileRows - 1
          if(!options.extend && x > self.rows - 1) self.rows - 1
          else x
        }
        val gb = GridBounds(firstCol, firstRow, lastCol, lastRow)
        tiles(layoutRow * tileLayout.layoutCols + layoutCol) =
          if(options.cropped) CroppedTile(self, gb)
          else CroppedTile(self, gb).toArrayTile
      }
    }

    tiles
  }

}
