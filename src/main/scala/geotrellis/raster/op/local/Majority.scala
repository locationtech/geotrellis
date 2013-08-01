package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster.IntArrayRasterData
import geotrellis.logic.Collect

import scala.collection.mutable
import spire.syntax._

object Majority {
  def apply(rs:Op[Raster]*):Majority =
    Majority(Collect(rs),0)

  def apply(level:Op[Int],rs:Op[Raster]*):Majority =
    Majority(Collect(rs),level)

  def apply(rasters:Op[Seq[Raster]]):Majority =
    Majority(rasters,0)
}

case class Majority(rasters:Op[Seq[Raster]],level:Op[Int]) extends Op2(rasters,level)({ 
  (rs,level) =>
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

    val counts = mutable.Map[Int,Int]()

    var col = 0
    while(col < cols) {
      var row = 0
      while(row < rows) {
        counts.clear
        for(r <- rs) {
          val v = r.get(col,row)
          if(v != NODATA) {
            if(!counts.contains(v)) {
              counts(v) = 1
            } else {
              counts(v) += 1
            }
          }
        }

        val sorted = 
          counts.keys
                .toSeq
                .sortBy { k => counts(k) }
                .toList
        val len = sorted.length - 1
        val m = 
          if(len >= level) { sorted(len-level) }
          else { NODATA }
        data.set(col,row, m)
        row += 1
      }
      col += 1
    }
    Result(Raster(data,re))
})
