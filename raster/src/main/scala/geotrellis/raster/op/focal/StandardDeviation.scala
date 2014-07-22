package geotrellis.raster.op.focal

import geotrellis.raster._

/**
 * Computes the standard deviation of a neighborhood for a given raster. Returns a raster of TypeDouble.
 *
 * @note            StandardDeviation does not currently support Double raster data inputs.
 *                  If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                  the data values will be rounded to integers.
 */
object StandardDeviation {

  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint) {
      new CursorCalculation[Tile](tile, n, bounds)
        with DoubleArrayTileResult
      {
        var count: Int = 0
        var sum: Double = 0

        def calc(r: Tile, c: Cursor) = {
          c.removedCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if(isData(v)) { count -= 1; sum -= v }
          }

          c.addedCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if(isData(v)) { count += 1; sum += v }
          }

          val mean = sum / count.toDouble
          var squares = 0.0

          c.allCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if(isData(v)) {
              squares += math.pow(v - mean, 2)
            }
          }

          tile.setDouble(c.col, c.row, math.sqrt(squares / count.toDouble))
        }
      }
    } else {
      new CursorCalculation[Tile](tile, n, bounds)
        with DoubleArrayTileResult
      {
        var count: Int = 0
        var sum: Int = 0

        def calc(r: Tile, c: Cursor) = {
          c.removedCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if (isData(v)) {
              count -= 1; sum -= v
            }
          }

          c.addedCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if (isData(v)) {
              count += 1; sum += v
            }
          }

          val mean = sum / count.toDouble
          var squares = 0.0

          c.allCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if (isData(v)) {
              squares += math.pow(v - mean, 2)
            }
          }
          tile.setDouble(c.col, c.row, math.sqrt(squares / count.toDouble))
        }
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, bounds).execute()
}
