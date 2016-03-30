package geotrellis.raster.split

import geotrellis.raster._

import spire.syntax.cfor._

import Split.Options

trait RasterExtentSplitMethods extends SplitMethods[RasterExtent] {
  def split(tileLayout: TileLayout, options: Options): Array[RasterExtent] = {
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    val splits = Array.ofDim[RasterExtent](tileLayout.layoutCols * tileLayout.layoutRows)
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
        splits(layoutRow * tileLayout.layoutCols + layoutCol) =
          self.rasterExtentFor(gb)
      }
    }

    splits
  }
}
