package geotrellis.raster.transform

import geotrellis.raster._
import spire.syntax.cfor._

trait TransformTileMethods extends TransformMethods[Tile] {
  def rotate90(n: Int = 1): Tile = {
    val (rows, cols) = self.rows -> self.cols
    if (n % 4 == 0) self
    else if (n % 2 == 0) {
      val tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
      if (!self.cellType.isFloatingPoint) {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.set(cols - 1 - col, rows - 1 - row, self.get(col, row))
          }
        }
      } else {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.setDouble(cols - 1 - col, rows - 1 - row, self.getDouble(col, row))
          }
        }
      }
      tile
    } else if (n % 3 == 0) {
      val tile = ArrayTile.alloc(self.cellType, self.rows, self.cols)

      if (!self.cellType.isFloatingPoint) {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.set(rows - 1 - row, col, self.get(col, row))
          }
        }
      } else {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.setDouble(rows - 1 - row, col, self.getDouble(col, row))
          }
        }
      }

      tile
    } else {
      val tile = ArrayTile.alloc(self.cellType, self.rows, self.cols)
      if (!self.cellType.isFloatingPoint) {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.set(row, cols - 1 - col, self.get(col, row))
          }
        }
      } else {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.setDouble(row, cols - 1 - col, self.getDouble(col, row))
          }
        }
      }

      tile
    }
  }

  def flipVertical: Tile = {
    val (rows, cols) = self.rows -> self.cols
    val tile = ArrayTile.alloc(self.cellType, cols, rows)

    if (!self.cellType.isFloatingPoint) {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.set(cols - 1 - col, row, self.get(col, row))
          tile.set(col, row, self.get(cols - col - 1, row))
        }
      }
    } else {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.setDouble(cols - 1 - col, row, self.getDouble(col, row))
          tile.setDouble(col, row, self.getDouble(cols - col - 1, row))
        }
      }
    }

    tile
  }

  def flipHorizontal: Tile = {
    val (rows, cols) = self.rows -> self.cols
    val tile = ArrayTile.alloc(self.cellType, cols, rows)

    if (!self.cellType.isFloatingPoint) {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.set(col, rows - 1 - row, self.get(col, row))
          tile.set(col, row, self.get(col, rows - 1 - row))
        }
      }
    } else {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.setDouble(col, rows - 1 - row, tile.getDouble(col, row))
          tile.setDouble(col, row, tile.getDouble(col, rows - 1 - row))
        }
      }
    }

    tile
  }

  def zscore: Tile = {
    if(!self.cellType.isFloatingPoint) {
      val stats = self.statistics.getOrElse(sys.error("No stats for a tile."))
      self.mapIfSet(z => ((z - stats.mean) / stats.stddev).toInt)
    } else {
      val stats = self.statisticsDouble.getOrElse(sys.error("No stats for a tile."))
      self.mapIfSetDouble(z => ((z - stats.mean) / stats.stddev).toInt)
    }
  }
}
