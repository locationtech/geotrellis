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
                  data.getDouble(y, x - math.signum(x-i))
                }else if (yVal.isValidInt) {
                  data.getDouble(yInt, x - math.signum(x-i))
                }else { // need linear interpolation
                  (yInt + 1 - yVal) * data.getDouble(yInt, x - math.signum(x-i)) + (yVal - yInt) *  data.getDouble(yInt + 1, x - math.signum(x-i))
                }
              }
              if (x > i) {
                data.setDouble(y,x,math.max(z, 1.0/(i-(x-1))*(k-closestHeight)+closestHeight))
              }else {
                data.setDouble(y,x,math.max(z, -1.0/(i-(x+1))*(k-closestHeight) + closestHeight))
              }
            }
        }

        val yPoints = yPointsPossible.filter {case (x,y) => inBounds(x,y)}
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
                  data.getDouble(y - math.signum(y-j), x)
                }else if (xVal.isValidInt) {
                  data.getDouble(y - math.signum(y-j), xInt)
                } else { // need linear interpolation
                  (xInt + 1 - xVal) *  data.getDouble(y - math.signum(y-j), xInt) + (xVal - xInt) * data.getDouble(y - math.signum(y-j), xInt + 1)
                }
              }
              if (y > j) {
                data.setDouble(y,x,math.max(z, 1.0/(j-(y-1))*(k-closestHeight) + closestHeight))
              }else {
                data.setDouble(y,x,math.max(z, -1.0/(j-(y+1))*(k-closestHeight) + closestHeight))
              }
            }
        }
      }
      ArrayRaster(data,re)
    }
  }

}

