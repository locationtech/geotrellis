package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.logic.Collect

import spire.syntax._

/**
 * Variety gives the count of unique values at each location in a set of Rasters.
 */
object Variety {
  def apply(rs:Op[Raster]*):Variety =
    Variety(Collect(rs))
}

/**
 * Variety gives the count of unique values at each location in a set of Rasters.
 */
case class Variety(rasters:Op[Seq[Raster]]) extends Op1(rasters) ({
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

    val data = IntArrayRasterData.empty(cols,rows)

    var col = 0
    while(col < cols) {
      var row = 0
      while(row < rows) {
        val variety =
          rs.map(r => r.get(col,row))
            .toSet
            .filter(_ != NODATA)
            .size
        data.set(col,row, 
          if(variety == 0) { NODATA } else { variety })
        row += 1
      }
      col += 1
    }
    Result(Raster(data,re))
})
