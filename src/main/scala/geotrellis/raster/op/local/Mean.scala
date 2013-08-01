package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.logic.Collect

import spire.syntax._

/**
 * The mean of values at each location in a set of Rasters.
 */
object Mean {
  def apply(rs:Op[Raster]*):Mean = 
    Mean(Collect(rs))
}

/**
 * The mean of values at each location in a set of Rasters.
 */
case class Mean(rasters:Op[Seq[Raster]]) extends Op1(rasters) ({
  (rs) => 
    rs.reduceLeft { (a,b) => 
      if(a.rasterExtent != b.rasterExtent) { 
        sys.error(s"Raster extents ${a.rasterExtent} and ${b.rasterExtent} are not equal") 
      }
      b
    }

    val re = rs(0).rasterExtent
    val cols = re.cols
    val rows = re.rows

    val data = DoubleArrayRasterData.empty(cols,rows)

    var count = 0
    var sum = 0.0
    val layerCount = rs.length
    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row => 
        sum = 0.0
        count = 0
        cfor(0)(_ < layerCount, _ + 1) { i =>
          val v = rs(i).getDouble(col,row)
          if(!java.lang.Double.isNaN(v)) {
            count += 1
            sum += v
          }
        }
        data.setDouble(col,row, sum / count)
      }
    }

    Result(Raster(data,re))
})
