package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.feature.Point

import scalaxy.loops._

/**
 * Created by jchien on 4/30/14.
 */
object ApproxViewshed extends Serializable {

  def apply(r: Tile, col: Int, row: Int): Tile = {
    r.localEqual(offsets(r, col, row))
  }

  def offsets(r: Tile, startCol: Int, startRow: Int): Tile = {
    val rows = r.rows
    val cols = r.cols

    if(startCol < 0 || startCol >= cols || startRow < 0 && startRow >= rows) {
      sys.error("Point indices out of bounds")
    } else {
      val k = r.getDouble(startCol, startRow)
      val tile = ArrayTile.alloc(TypeDouble,cols,rows)
      tile.setDouble(startCol, startRow, k)

      val maxLayer = List((rows - startRow), (cols - startCol), startRow + 1, startCol + 1).max

      for(layer <- 1 until maxLayer) {

        def doY(x: Int, y: Int): Unit =
          if(y >= 0 && y < rows && x >= 0 && x < cols) {
            val z = r.getDouble(x,y)

            if (layer == 1) {
              tile.setDouble(x,y,z)
            }else {
              val xVal = math.abs(1.0 / (startRow - y)) * (startCol - x) + x
              val xInt = xVal.toInt

              val closestHeight = {
                if (startRow == y) {
                  tile.getDouble(x, y - math.signum(y - startRow))
                } else if (xVal.isValidInt) {
                  tile.getDouble(xInt, y - math.signum(y - startRow))
                } else { // need linear interpolation
                  (xInt + 1 - xVal) * tile.getDouble(xInt, y - math.signum(y - startRow)) +
                  (xVal - xInt) *  tile.getDouble(xInt + 1, y - math.signum(y - startRow))
                }
              }

              if (y > startRow) {
                tile.setDouble(x, y,
                  math.max(z, 1.0 / (startRow - (y - 1)) * (k - closestHeight) + closestHeight)
                )
              } else {
                tile.setDouble(x, y,
                  math.max(z, -1.0 / (startRow - (y + 1)) * (k - closestHeight) + closestHeight)
                )
              }
            }
          }

        def doX(x: Int, y: Int): Unit =
          if(y >= 0 && y < rows && x >= 0 && x < cols) {
            val z = r.getDouble(x,y)
            if (layer == 1) {
              tile.setDouble(x,y,z)
            }else {
              val yVal = math.abs(1.0 / (startCol - x)) * (startRow - y) + y
              val yInt = yVal.toInt
              val closestHeight = {
                if (startCol == x) {
                  tile.getDouble(x - math.signum(x - startCol), y)
                }else if (yVal.isValidInt) {
                  tile.getDouble(x - math.signum(x - startCol), yInt)
                } else { // need linear interpolation
                  (yInt + 1 - yVal) *  tile.getDouble(x - math.signum(x-startCol), yInt) + 
                  (yVal - yInt) * tile.getDouble(x - math.signum(x-startCol), yInt + 1)
                }
              }

              if (x > startCol) {
                tile.setDouble(x, y, 
                  math.max(z, 1.0 / (startCol - (x - 1)) * (k - closestHeight) + closestHeight)
                )
              } else {
                tile.setDouble(x, y, 
                  math.max(z, -1.0 / (startCol - (x+1)) * (k-closestHeight) + closestHeight)
                )
              }
            }
          }

        for(ii <- 0 until (2 * layer) optimized) {
          doY(startCol - layer + ii, startRow - layer)
          doY(startCol + layer - ii, startRow + layer)
          doX(startCol - layer, startRow + layer - ii)
          doX(startCol + layer, startRow - layer + ii)
        }
      }

      tile
    }
  }
}

