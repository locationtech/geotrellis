package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.FocalTarget.FocalTarget

object Conway {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    new CellwiseCalculation[Tile](tile, n, FocalTarget.All, bounds)
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

      def setValue(x: Int, y: Int) = resultTile.set(x, y, if(count == 3 || count == 2) 1 else NODATA)

      def reset() = { count = 0 }

      def copy(focusCol: Int, focusRow: Int, x: Int, y: Int) =
        resultTile.setDouble(x, y, tile.getDouble(focusCol, focusRow))
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute()
}
