package geotrellis.raster.op.global

import geotrellis._
import geotrellis.feature.Point
import geotrellis.raster.RasterData

import spire.syntax.cfor._

/**
 * Created by jchien on 4/30/14.
 */
object ApproxViewshed extends Serializable {

  def apply(r: Raster, p: Point): Raster = {
    val (col, row) = r.rasterExtent.mapToGrid(p.x, p.y)
    apply(r, col, row)
  }

  def apply(r: Raster, col: Int, row: Int): Raster = {
    r.localEqual(offsets(r, col, row))
  }

  def offsets(r:Raster, p: Point): Raster = {
    val (col, row) = r.rasterExtent.mapToGrid(p.x, p.y)
    offsets(r, col, row)
  }

  def offsets(r: Raster, startCol: Int, startRow: Int): Raster = {
    val re = r.rasterExtent
    val rows = re.rows
    val cols = re.cols

    if(startCol < 0 || startCol >= cols || startRow < 0 && startRow >= rows) {
      sys.error("Point indices out of bounds")
    } else {
      val k = r.getDouble(startCol, startRow)
      val data = RasterData.allocByType(TypeDouble,cols,rows)
      data.setDouble(startCol, startRow, k)

      val maxLayer = List((rows - startRow), (cols - startCol), startRow + 1, startCol + 1).max

      for(layer <- 1 until maxLayer) {

        def doY(x: Int, y: Int): Unit =
          if(y >= 0 && y < rows && x >= 0 && x < cols) {
            val z = r.getDouble(x,y)

            if (layer == 1) {
              data.setDouble(x,y,z)
            }else {
              val xVal = math.abs(1.0 / (startRow - y)) * (startCol - x) + x
              val xInt = xVal.toInt

              val closestHeight = {
                if (startRow == y) {
                  data.getDouble(x, y - math.signum(y - startRow))
                } else if (xVal.isValidInt) {
                  data.getDouble(xInt, y - math.signum(y - startRow))
                } else { // need linear interpolation
                  (xInt + 1 - xVal) * data.getDouble(xInt, y - math.signum(y - startRow)) +
                  (xVal - xInt) *  data.getDouble(xInt + 1, y - math.signum(y - startRow))
                }
              }

              if (y > startRow) {
                data.setDouble(x, y,
                  math.max(z, 1.0 / (startRow - (y - 1)) * (k - closestHeight) + closestHeight)
                )
              } else {
                data.setDouble(x, y,
                  math.max(z, -1.0 / (startRow - (y + 1)) * (k - closestHeight) + closestHeight)
                )
              }
            }
          }

        def doX(x: Int, y: Int): Unit =
          if(y >= 0 && y < rows && x >= 0 && x < cols) {
            val z = r.getDouble(x,y)
            if (layer == 1) {
              data.setDouble(x,y,z)
            }else {
              val yVal = math.abs(1.0 / (startCol - x)) * (startRow - y) + y
              val yInt = yVal.toInt
              val closestHeight = {
                if (startCol == x) {
                  data.getDouble(x - math.signum(x - startCol), y)
                }else if (yVal.isValidInt) {
                  data.getDouble(x - math.signum(x - startCol), yInt)
                } else { // need linear interpolation
                  (yInt + 1 - yVal) *  data.getDouble(x - math.signum(x-startCol), yInt) + 
                  (yVal - yInt) * data.getDouble(x - math.signum(x-startCol), yInt + 1)
                }
              }

              if (x > startCol) {
                data.setDouble(x, y, 
                  math.max(z, 1.0 / (startCol - (x - 1)) * (k - closestHeight) + closestHeight)
                )
              } else {
                data.setDouble(x, y, 
                  math.max(z, -1.0 / (startCol - (x+1)) * (k-closestHeight) + closestHeight)
                )
              }
            }
          }

        cfor(0)(_ < (2 * layer), _ + 1) { ii => 
          doY(startCol - layer + ii, startRow - layer)
          doY(startCol + layer - ii, startRow + layer)
          doX(startCol - layer, startRow + layer - ii)
          doX(startCol + layer, startRow - layer + ii)
        }
      }

      ArrayRaster(data,re)
    }
  }
}

