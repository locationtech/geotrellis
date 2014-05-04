package geotrellis.raster.op.global

import geotrellis.{TypeBit, TypeDouble, ArrayRaster, Raster}
import geotrellis.raster.RasterData

/**
 * Created by jchien on 4/30/14.
 */
object ApproxViewshed extends Serializable {

  def getRasterVal(col:Int,row:Int,r:Raster):Double = {
    if(r.rasterType.isDouble) {
      r.getDouble(col, row)
    }else {
      r.get(col, row).toDouble
    }
  }

  def approxComputeViewable(i:Int,j:Int,r:Raster):Raster = {
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
    val requiredHeights = approxComputeMinHeightViewable(i, j, r)

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

  def approxComputeMinHeightViewable(i:Int,j:Int,r:Raster):Raster = {
    val re = r.rasterExtent
    val rows = re.rows
    val cols = re.cols
    def inBounds(x:Int, y:Int):Boolean = (x >= 0 && x < rows && y >= 0 && y < cols)

    if(! inBounds(i,j)) {
      sys.error("Point indices out of bounds")
    } else {
      val k = getRasterVal(j,i,r)
      val data = RasterData.allocByType(TypeDouble,cols,rows)
      data.setDouble(j, i, k)
      val maxLayer = List((rows - i), (cols - j), i + 1, j + 1).max
      for(layer <- 1 until maxLayer) {
        var xPointsPossible = List.empty[Pair[Int, Int]]
        var yPointsPossible = List.empty[Pair[Int, Int]]
        for(ii <- 0 until (2*layer)) {
          xPointsPossible = Pair(i - layer,j - layer + ii) :: Pair(i + layer,j + layer - ii) :: xPointsPossible
          yPointsPossible = Pair(i + layer - ii,j - layer) :: Pair(i - layer + ii,j + layer) :: yPointsPossible
        }

        val xPoints = xPointsPossible.filter {case (x,y) => inBounds(x,y)}
        System.out.println("Mapping xPoints: " + xPointsPossible.toString)
        xPoints.map {
          case (x,y) =>
            val z = getRasterVal(y,x,r)
            if (layer == 1) {
              data.setDouble(y,x,z)
            }else {
              val yVal = math.abs(1.0/(i-x))*(j-y) + y
              val yInt = yVal.toInt
              val closestHeight = {
                if (i == x) {
                  getRasterVal(y, x - math.signum(x-i), r)
                }else if (yVal.isValidInt) {
                  getRasterVal(yInt, x - math.signum(x-i), r)
                }else { // need linear interpolation
                  (yInt + 1 - y) * getRasterVal(yInt, x - math.signum(x-i), r) + (y - yInt) * getRasterVal(yInt + 1, x - math.signum(x-i), r)
                }
              }
              if (x > i) {
                data.setDouble(y,x,4)
                // data.setDouble(y,x,math.max(z, 1.0/(i-(x-1))*(k-closestHeight)+closestHeight))
              }else {
                data.setDouble(y,x,2)
                // data.setDouble(y,x,math.max(z, -1.0/(i-(x+1))*(k-closestHeight) + closestHeight))
              }
            }
        }

        val yPoints = xPointsPossible.filter {case (x,y) => inBounds(x,y)}
        System.out.println("Mapping yPoints: " + yPointsPossible.toString)
        yPoints.map {
          case (x,y) =>
            val z = getRasterVal(y,x,r)
            if (layer == 1) {
              data.setDouble(y,x,z)
            }else {
              val xVal = math.abs(1.0/(j-y))*(i-x) + x
              val xInt = xVal.toInt
              val closestHeight = {
                if (j == y) {
                  getRasterVal(y - math.signum(y-j), x, r)
                }else if (xVal.isValidInt) {
                  getRasterVal(y - math.signum(y-j), xInt, r)
                } else { // need linear interpolation
                  (xInt + 1 - x) * getRasterVal(y - math.signum(y-j), xInt,r) + (x - xInt) * getRasterVal(y - math.signum(y-j), xInt + 1, r)
                }
              }
              if (y > j) {
                data.setDouble(y,x,5)
                // data.setDouble(y,x,math.max(z, 1.0/(j-(y-1))*(k-closestHeight) + closestHeight))
              }else {
                data.setDouble(y,x,3)
                // data.setDouble(y,x,math.max(z, -1.0/(j-(y+1))*(k-closestHeight) + closestHeight))
              }
            }
        }
      }
      ArrayRaster(data,re)
    }
  }

}

