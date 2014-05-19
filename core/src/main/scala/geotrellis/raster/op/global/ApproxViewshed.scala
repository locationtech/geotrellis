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
    r.localEqual(approxComputeMinHeightViewable(i, j, r))
  }

  def approxComputeMinHeightViewable(i:Int,j:Int,r:Raster):Raster = {
    val re = r.rasterExtent
    val rows = re.rows
    val cols = re.cols
    def inBounds(x:Int, y:Int):Boolean = (y >= 0 && y < rows && x >= 0 && x < cols)

    if(! inBounds(i,j)) {
      sys.error("Point indices out of bounds")
    } else {
      val k = getRasterVal(i,j,r)
      val data = RasterData.allocByType(TypeDouble,cols,rows)
      data.setDouble(i, j, k)
      val maxLayer = List((rows - j), (cols - i), j + 1, i + 1).max
      for(layer <- 1 until maxLayer) {
        var yPointsPossible = List.empty[Pair[Int, Int]]
        var xPointsPossible = List.empty[Pair[Int, Int]]
        for(ii <- 0 until (2*layer)) {
          yPointsPossible = Pair(i - layer + ii,j - layer) :: Pair(i + layer - ii,j + layer) :: yPointsPossible
          xPointsPossible = Pair(i - layer,j + layer - ii) :: Pair(i + layer,j - layer + ii) :: xPointsPossible
        }

        val yPoints = yPointsPossible.filter {case (x,y) => inBounds(x,y)}
        yPoints.map {
          case (x,y) =>
            val z = getRasterVal(x,y,r)
            if (layer == 1) {
              data.setDouble(x,y,z)
            }else {
              val xVal = math.abs(1.0/(j-y))*(i-x) + x
              val xInt = xVal.toInt
              val closestHeight = {
                if (j == y) {
                  data.getDouble(x, y - math.signum(y-j))
                }else if (xVal.isValidInt) {
                  data.getDouble(xInt, y - math.signum(y-j))
                }else { // need linear interpolation
                  (xInt + 1 - xVal) * data.getDouble(xInt, y - math.signum(y-j)) + (xVal - xInt) *  data.getDouble(xInt + 1, y - math.signum(y-j))
                }
              }
              if (y > j) {
                data.setDouble(x,y,math.max(z, 1.0/(j-(y-1))*(k-closestHeight) + closestHeight))
              }else {
                data.setDouble(x,y,math.max(z, -1.0/(j-(y+1))*(k-closestHeight) + closestHeight))
              }
            }
        }

        val xPoints = xPointsPossible.filter {case (x,y) => inBounds(x,y)}
        xPoints.map {
          case (x,y) =>
            val z = getRasterVal(x,y,r)
            if (layer == 1) {
              data.setDouble(x,y,z)
            }else {
              val yVal = math.abs(1.0/(i-x))*(j-y) + y
              val yInt = yVal.toInt
              val closestHeight = {
                if (i == x) {
                  data.getDouble(x - math.signum(x-i), y)
                }else if (yVal.isValidInt) {
                  data.getDouble(x - math.signum(x-i), yInt)
                } else { // need linear interpolation
                  (yInt + 1 - yVal) *  data.getDouble(x - math.signum(x-i), yInt) + (yVal - yInt) * data.getDouble(x - math.signum(x-i), yInt + 1)
                }
              }
              if (x > i) {
                data.setDouble(x,y,math.max(z, 1.0/(i-(x-1))*(k-closestHeight) + closestHeight))
              }else {
                data.setDouble(x,y,math.max(z, -1.0/(i-(x+1))*(k-closestHeight) + closestHeight))
              }
            }
        }
      }
      ArrayRaster(data,re)
    }
  }

}

