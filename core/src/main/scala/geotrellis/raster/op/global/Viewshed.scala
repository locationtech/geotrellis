package geotrellis.raster.op.global

import geotrellis.{TypeBit, TypeDouble, ArrayRaster, Raster}
import geotrellis.raster.RasterData

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

    for(col <- 0 until cols) {
      for(row <- 0 until rows) {
        if (height >= requiredHeights.getDouble(col, row)) {
          data.set(col, row, 1)
        } else {
          data.set(col, row, 0)
        }
      }
    }
    ArrayRaster(data,re)
  }

  def computeHeightRequired(i:Int,j:Int,r:Raster):Raster = {
    val re = r.rasterExtent
    val rows = re.rows
    val cols = re.cols

    if(i >= rows || i < 0 || j >= cols || j < 0) {
      sys.error("Point indices out of bounds")
    } else {
      val data = RasterData.allocByType(TypeDouble,cols,rows)

      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          val height = { // (row, col, height) is the point we're checking is viewable
            if(r.rasterType.isDouble) {
              r.getDouble(col, row)
            }else {
              r.get(col,row)
            }
          }

          // Line thru (x1,y1,z1) & (x2,y2,z2) is defined by (x-x1)/(x2-x1) = (y-y1)/(y2-y1) = (z-z1)/(z2-z1)
          val xLatticeVals = getRangeBetween(i, row)
          val xLatticeYVals = xLatticeVals.map(x => (x - i).toDouble / (row - i) * (col - j) + j)
          val xLatticeZVals = (xLatticeVals, xLatticeYVals).zipped.map( (x, y) => {
            val yInt = y.toInt
            val z = { // (x,y,z) is the point in between
              if (y.isValidInt) {
                if(r.rasterType.isDouble) {
                  r.getDouble(yInt, x)
                }else {
                  r.get(yInt, x)
                }
              } else { // need linear interpolation
                if(r.rasterType.isDouble) {
                  (yInt + 1 - y) * r.getDouble(yInt, x) + (y - yInt) * r.getDouble(yInt + 1, x)
                }else {
                  (yInt + 1 - y) * r.get(yInt, x) + (y - yInt) * r.get(yInt + 1, x)
                }
              }
            }
            val requiredHeight = (i - row).toDouble / (x - row) * (z - height) + height

            // for debugging only
            if (y - yInt < 0 || yInt < 0) {print(y); throw new RuntimeException("Rounding Error")}
            if (y != col) {
              val requiredHeight2 = (j - col).toDouble / (y - col) * (z - height) + height // should be the same
              if (Math.abs(requiredHeight - requiredHeight2) > 0.001) { throw new RuntimeException("Unequal computation") }
            }
            // end debugging block

            requiredHeight
          })

          val yLatticeVals = getRangeBetween(j, col)
          val yLatticeXVals = yLatticeVals.map(y => (y - j).toDouble / (col - j) * (row - i) + i)
          val yLatticeZVals = (yLatticeXVals, yLatticeVals).zipped.map( (x, y) => {
            val xInt = x.toInt
            val z = { // (x,y,z) is the point in between
              if (x.isValidInt) {
                if(r.rasterType.isDouble) {
                  r.getDouble(y, xInt)
                }else {
                  r.get(y, xInt)
                }
              } else { // need linear interpolation
                if(r.rasterType.isDouble) {
                  (xInt + 1 - x) * r.getDouble(y, xInt) + (x - xInt) * r.getDouble(y, xInt + 1)
                }else {
                  (xInt + 1 - x) * r.get(y, xInt) + (x - xInt) * r.get(y, xInt + 1)
                }
              }
            }
            val requiredHeight = (j - col).toDouble / (y - col) * (z - height) + height

            // for debugging only
            if (x - xInt < 0 || xInt < 0) {print(x); throw new RuntimeException("Rounding Error")}
            if (x != row) {
              val requiredHeight2 = (i - row).toDouble / (x - row) * (z - height) + height // should be the same if x != row
              if (Math.abs(requiredHeight - requiredHeight2) > 0.001) { throw new RuntimeException("Unequal computation") }
            }
            // end testing block

            requiredHeight
          })

          if (xLatticeZVals.isEmpty && yLatticeZVals.isEmpty) {
            data.setDouble(col, row, Double.MinValue)
          } else if (xLatticeZVals.isEmpty) {
            data.setDouble(col, row, yLatticeZVals.max)
          } else if (yLatticeZVals.isEmpty) {
            data.setDouble(col, row, xLatticeZVals.max)
          } else {
            data.setDouble(col, row, Math.max(xLatticeZVals.max, yLatticeZVals.max))
          }
        }
      }
      ArrayRaster(data,re)
    }
  }

  def getRangeBetween(a:Int,b:Int):Seq[Int] = {
    if (a < b) {
      Range(a+1, b, 1)
    } else if (a > b) {
      Range(b+1, a, 1)
    } else {
      Seq.empty[Int]
    }
  }

}

