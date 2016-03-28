package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell

/**
 * Computes the standard deviation of a neighborhood for a given raster. Returns a raster of DoubleConstantNoDataCellType.
 *
 * @note            StandardDeviation does not currently support Double raster data inputs.
 *                  If you use a Tile with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
 *                  the data values will be rounded to integers.
 */
object StandardDeviation {

  def calculation(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint) {
      new CursorCalculation[Tile](tile, n, target, bounds)
        with DoubleArrayTileResult
      {
        var count: Int = 0
        var sum: Double = 0

        def calc(tile: Tile, c: Cursor) = {
          c.removedCells.foreach { (x, y) =>
            val v = tile.getDouble(x, y)
            if(isData(v)) {
              count -= 1
              if (count == 0) sum = 0 else sum -= v
            }
          }

          c.addedCells.foreach { (x, y) =>
            val v = tile.getDouble(x, y)
            if(isData(v)) { count += 1; sum += v }
          }

          val mean = sum / count.toDouble
          var squares = 0.0

          c.allCells.foreach { (x, y) =>
            val v = tile.getDouble(x, y)
            if(isData(v)) {
              squares += math.pow(v - mean, 2)
            }
          }

          setValidDoubleResult(c, math.sqrt(squares / count.toDouble))
        }
      }
    } else {
      new CursorCalculation[Tile](tile, n, target, bounds)
        with DoubleArrayTileResult
      {
        var count: Int = 0
        var sum: Int = 0

        def calc(tile: Tile, c: Cursor) = {
          c.removedCells.foreach { (x, y) =>
            val v = tile.get(x, y)
            if (isData(v)) {
              count -= 1; sum -= v
            }
          }

          c.addedCells.foreach { (x, y) =>
            val v = tile.get(x, y)
            if (isData(v)) {
              count += 1; sum += v
            }
          }

          val mean = sum / count.toDouble
          var squares = 0.0

          c.allCells.foreach { (x, y) =>
            val v = tile.get(x, y)
            if (isData(v)) {
              squares += math.pow(v - mean, 2)
            }
          }

          setValidDoubleResultFromInt(c, math.sqrt(squares / count.toDouble))
        }
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, target: TargetCell = TargetCell.All, bounds: Option[GridBounds] = None): Tile =
    calculation(tile, n, target, bounds).execute()
}
