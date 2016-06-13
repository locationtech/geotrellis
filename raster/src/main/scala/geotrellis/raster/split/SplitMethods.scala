package geotrellis.raster.split

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import Split.Options

trait SplitMethods[T <: Grid] extends MethodExtensions[T] {
  /**
    * Splits this into an array of elements based on a TileLayout.
    * The array will be in row order form such that the top left element is first.
    *
    * @param        tileLayout     TileLayout defining the tiles to be generated
    *
    * @return                      An array of T
    */
  def split(tileLayout: TileLayout): Array[T] =
    split(tileLayout, Options.DEFAULT)

  /**
    * Splits this into an array of elements based on a TileLayout.
    * The array will be in row order form such that the top left element is first.
    *
    * @param        tileLayout     TileLayout defining the tiles to be generated
    * @param        options        Options that control the split
    *
    * @return                      An array of T
    */
  def split(tileLayout: TileLayout, options: Options): Array[T]

  /**
    * Splits this into an array of elements based on a columns and rows to be in each tile.
    * The array will be in row order form such that the top left element is first.
    *
    * @param        cols           Column count of output tiles
    * @param        rows           Row count of output tiles
    * @param        options        Options that control the split
    *
    * @return                      An array of T
    */
  def split(cols: Int, rows: Int): Array[T] =
    split(TileLayout(
            layoutCols = math.ceil(self.cols.toDouble / cols.toDouble).toInt,
            layoutRows = math.ceil(self.rows.toDouble / rows.toDouble).toInt,
            tileCols = cols,
            tileRows = rows))

  /**
    * Splits this into an array of elements into square tiles.
    * The array will be in row order form such that the top left element is first.
    *
    * @param        cols           Column and row count of output tiles
    * @param        options        Options that control the split
    *
    * @return                      An array of T
    */
  def split(cells: Int): Array[T] = split(cells, cells)
}
