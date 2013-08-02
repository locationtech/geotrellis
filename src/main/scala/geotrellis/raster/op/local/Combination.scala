package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster.IntArrayRasterData
import geotrellis.logic.Collect
import scala.collection.mutable
import spire.syntax._

object Combination {
  def apply(rs:Op[Raster]*):Combination =
    Combination(Collect(rs))
}

case class Combination(rasters:Op[Seq[Raster]]) extends Op1(rasters)({
  (rasters) =>
    val rs = rasters.toArray
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

    val combos = mutable.ListBuffer[Array[Int]]()
    var combosLength = 0
    val len = rs.length
  
    // Used to compare combinations. Assumes same length
    def compareArray(a1:Array[Int],a2:Array[Int]):Boolean = {
      var i = 0
      while(i < len) { if(a1(i) != a2(i)) return false ; i += 1 }
      return true
    }

    val stagingArray = Array.ofDim[Int](len)

    var col = 0
    while(col < cols) {
      var row = 0
      while(row < rows) {
        cfor(0)(_<len,_+1) { i => stagingArray(i) = rs(i).get(col,row) }
        var c = 0
        var found = false
        while(c < combosLength && !found) { 
          if(compareArray(stagingArray,combos(c))) {
            found = true
            data.set(col,row,c)
          }
          c += 1
        }
        if(!found) {
          data.set(col,row,c)
          combos += stagingArray.clone
          combosLength += 1
        }
        row += 1
      }
      col += 1
    }
    Result(Raster(data,re))
})
