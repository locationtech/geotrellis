package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

object Conway {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    new CellwiseCalculation[Tile](tile, n, TargetCell.All, bounds)
      with ByteArrayTileResult
    {
      var count = 0

      def add(tile: Tile, x: Int, y: Int) = {
        val z = tile.get(x, y)
        if (isData(z)) {
          count += 1
        }
      }

      def remove(tile: Tile, x: Int, y: Int) = {
        val z = tile.get(x, y)
        if (isData(z)) {
          count -= 1
        }
      }

      def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int) =
        setValidResult(x, y, focusCol, focusRow, if(count == 3 || count == 2) 1 else NODATA)

      def reset() = { count = 0 }

    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute()
}
