package geotrellis.raster.op.global

import geotrellis.{TypeBit, TypeDouble, ArrayRaster, Raster}
import geotrellis.raster.RasterData

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
        r.getDouble(j, i)
      }else {
        r.get(j, i)
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


  def computeHeightRequired(i:Int,j:Int,r:Raster):Raster = {
    val re = r.rasterExtent
    val rows = re.rows
    val cols = re.cols

    if(i >= rows || i < 0 || j >= cols || j < 0) {
      sys.error("Point indices out of bounds")
    } else {
      val data = RasterData.allocByType(TypeDouble,cols,rows)

      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          val height = r.getDouble(col, row)

          // Line thru (x1,y1,z1) & (x2,y2,z2) is defined by (x-x1)/(x2-x1) = (y-y1)/(y2-y1) = (z-z1)/(z2-z1)
          var max = Double.MinValue

          if(i != row) {
            val (rowMin, rowMax) =
              if (i < row) {
                (i+1, row)
              } else {
                (row+1, i)
              }

            for( x <- rowMin to rowMax optimized) {
              val y = (x - i).toDouble / (row - i) * (col - j) + j

              val yInt = y.toInt
              val z = { // (x,y,z) is the point in between
                if (y.isValidInt) {
                  r.getDouble(yInt, x)
                } else { // need linear interpolation
                  (yInt + 1 - y) * r.getDouble(yInt, x) + (y - yInt) * r.getDouble(yInt + 1, x)
                }
              }
              val requiredHeight = (i - row).toDouble / (x - row) * (z - height) + height
              if(requiredHeight > max) { max = requiredHeight }
            }
          }

          if(j != col) {
            val (colMin, colMax) =
              if (j < col) {
                (j+1, col)
              } else {
                (col+1, j)
              }

            for ( y <- colMin to colMax optimized) {
              val x = (y - j).toDouble / (col - j) * (row - i) + i

              val xInt = x.toInt
              val z = { // (x,y,z) is the point in between
                if (x.isValidInt) {
                  r.getDouble(y, xInt)
                } else { // need linear interpolation
                  (xInt + 1 - x) * r.getDouble(y, xInt) + (x - xInt) * r.getDouble(y, xInt + 1)
                }
              }
              val requiredHeight = (j - col).toDouble / (y - col) * (z - height) + height
              if(requiredHeight > max) { max = requiredHeight }
            }
          }

          data.setDouble(col, row, max)
        }
      }
      ArrayRaster(data,re)
    }
  }
}

