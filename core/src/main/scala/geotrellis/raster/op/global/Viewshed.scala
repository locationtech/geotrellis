package geotrellis.raster.op.global

import geotrellis.raster.RasterData
import geotrellis._

import scalaxy.loops._

/**
 * Created by jchien on 4/24/14.
 */
object Viewshed extends Serializable {

  def computeViewable(i:Int,j:Int,r:Raster):Raster = {
    val re = r.rasterExtent
    val rows = re.rows
    val cols = re.cols
    val data = RasterData.allocByType(TypeBit,cols,rows)
    val height = {
      if(r.rasterType.isDouble) {
        r.getDouble(i, j)
      }else {
        r.get(i, j)
      }
    }
    val requiredHeights = computeHeightRequired(i, j, r)

    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        if (height >= requiredHeights.getDouble(col, row)-0.5) {
          data.set(col, row, 1)
        } else {
          data.set(col, row, 0)
        }
      }
    }
    ArrayRaster(data,re)
  }

// [info]            benchmark   ms linear runtime
// [info] CornerRequiredHeight 1582 ==============================
// [info] CenterRequiredHeight  732 =============

// [info] CornerRequiredHeight 685 ==============================
// [info] CenterRequiredHeight 343 ===============

  // i and x correspond to columns, y and j correspond to rows
  def computeHeightRequired(i:Int,j:Int,r:Raster):Raster = {
    val re = r.rasterExtent
    val rows = re.rows
    val cols = re.cols

    if(j >= rows || j < 0 || i >= cols || i < 0) {
      sys.error("Point indices out of bounds")
    } else {
      val data = RasterData.allocByType(TypeDouble,cols,rows)

      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          val height = r.getDouble(col, row)

          if (isNoData(height)) {
            data.setDouble(col, row, Double.NaN)
          }else {
            // Line thru (x1,y1,z1) & (x2,y2,z2) is defined by (x-x1)/(x2-x1) = (y-y1)/(y2-y1) = (z-z1)/(z2-z1)
            var max = Double.MinValue

            if(j != row) {
              val (rowMin, rowMax) =
                if (j < row) {
                  (j+1, row)
                } else {
                  (row+1, j)
                }

              for( y <- rowMin to rowMax optimized) {
                val x = (y - j).toDouble / (row - j) * (col - i) + i

                val xInt = x.toInt
                val z = { // (x,y,z) is the point in between
                  if (x.isValidInt){
                    r.getDouble(xInt, y)
                  }else { // need linear interpolation
                    (xInt + 1 - x) * r.getDouble(xInt, y) + (x - xInt) * r.getDouble(xInt + 1, y)
                  }
                }
                val requiredHeight = (j - row).toDouble / (y - row) * (z - height) + height
                if(requiredHeight > max) { max = requiredHeight }
              }
            }

            if(i != col) {
              val (colMin, colMax) =
                if (i < col) {
                  (i+1, col)
                } else {
                  (col+1, i)
                }

              for ( x <- colMin to colMax optimized) {
                val y = (x - i).toDouble / (col - i) * (row - j) + j

                val yInt = y.toInt
                val z = { // (x,y,z) is the point in between
                  if (y.isValidInt) {
                    r.getDouble(x, yInt)
                  } else { // need linear interpolation
                    (yInt + 1 - y) * r.getDouble(x, yInt) + (y - yInt) * r.getDouble(x, yInt + 1)
                  }
                }
                val requiredHeight = (i - col).toDouble / (x - col) * (z - height) + height
                if(requiredHeight > max) { max = requiredHeight }
              }
            }

            data.setDouble(col, row, max)
          }
        }
      }
      ArrayRaster(data,re)
    }
  }
}

