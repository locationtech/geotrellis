package geotrellis.raster.op.global

import geotrellis._
import geotrellis.feature.Point
import geotrellis.raster.ArrayTile

import scalaxy.loops._

/**
 * Created by jchien on 4/24/14.
 */
object Viewshed extends Serializable {
  def apply(r: Raster, startCol: Int, startRow: Int): Raster = {
    val (cols, rows) = r.dimensions
    val data = ArrayTile.allocByType(TypeBit,cols,rows)

    val height = r.getDouble(startCol, startRow)

    val requiredHeights = 
      offsets(r, startCol, startRow)

    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        if (height >= requiredHeights.getDouble(col, row) - 0.5) {
          data.set(col, row, 1)
        } else {
          data.set(col, row, 0)
        }
      }
    }

    ArrayRaster(data, cols, rows)
  }

  def offsets(r: Raster, startCol: Int, startRow: Int): Raster = {
    val (cols, rows) = r.dimensions

    if(startRow >= rows || startRow < 0 || startCol >= cols || startCol < 0) {
      sys.error("Point indices out of bounds")
    } else {
      val data = ArrayTile.allocByType(TypeDouble, cols, rows)

      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          val height = r.getDouble(col, row)

          if (isNoData(height)) {
            data.setDouble(col, row, Double.NaN)
          }else {
            // Line thru (x1, y1, z1) & (x2, y2, z2) is defined by 
            // (x-x1)/(x2-x1) = (y-y1)/(y2-y1) = (z-z1)/(z2-z1)
            var max = Double.MinValue

            if(startRow != row) {
              val (rowMin, rowMax) =
                if (startRow < row) {
                  (startRow + 1, row)
                } else {
                  (row + 1, startRow)
                }

              for( y <- rowMin to rowMax optimized) {
                val x = (y - startRow).toDouble / (row - startRow) * (col - startCol) + startCol

                val xInt = x.toInt
                val z = { 
                  // (x, y, z) is the point in between
                  if (x.isValidInt) {
                    r.getDouble(xInt, y)
                  } else { 
                    // need linear interpolation
                    (xInt + 1 - x) * r.getDouble(xInt, y) + (x - xInt) * r.getDouble(xInt + 1, y)
                  }
                }
                val requiredHeight = (startRow - row).toDouble / (y - row) * (z - height) + height
                if(requiredHeight > max) { max = requiredHeight }
              }
            }

            if(startCol != col) {
              val (colMin, colMax) =
                if (startCol < col) {
                  (startCol + 1, col)
                } else {
                  (col + 1, startCol)
                }

              for (x <- colMin to colMax optimized) {
                val y = (x - startCol).toDouble / (col - startCol) * (row - startRow) + startRow

                val yInt = y.toInt
                val z = { 
                  // (x, y, z) is the point in between
                  if (y.isValidInt) {
                    r.getDouble(x, yInt)
                  } else { 
                    // need linear interpolation
                    (yInt + 1 - y) * r.getDouble(x, yInt) + (y - yInt) * r.getDouble(x, yInt + 1)
                  }
                }
                val requiredHeight = (startCol - col).toDouble / (x - col) * (z - height) + height
                if(requiredHeight > max) { max = requiredHeight }
              }
            }

            data.setDouble(col, row, max)
          }
        }
      }
      ArrayRaster(data, cols, rows)
    }
  }
}
