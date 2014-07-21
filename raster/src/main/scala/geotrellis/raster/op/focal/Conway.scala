package geotrellis.raster.op.focal

import geotrellis.raster._

object Conway {
  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    new CellwiseCalculation[Tile](tile, n, bounds)
      with ByteArrayTileResult
    {
      var count = 0

      def add(r: Tile, x: Int, y: Int) = {
        val z = r.get(x, y)
        if (isData(z)) {
          count += 1
        }
      }

      def remove(r: Tile, x: Int, y: Int) = {
        val z = r.get(x, y)
        if (isData(z)) {
          count -= 1
        }
      }

      def setValue(x: Int, y: Int) = tile.set(x, y, if(count == 3 || count == 2) 1 else NODATA)
      def reset() = { count = 0 }
    }
  }
}
