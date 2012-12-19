package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics.{Statistics,FastMapHistogram}

/** Calculates spatial autocorrelation of cells based on the similarity to
 * neighboring values.
 *
 * The statistic for each focus in the resulting raster is such that the more
 * positive the number, the greater the similarity between the focus value and
 * it's neighboring values, and the more negative the number, the more dissimilar
 * the focus value is with it's neighboring values.
 *
 * @param       r         Raster to perform the operation on.
 * @param       n         Neighborhood to use in this focal operation.
 */
case class RasterMoransI(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp[Raster](r,n)({
  (r,n) => new CursorCalculation[Raster] with DoubleRasterDataResult {
    var mean = 0.0
    var `stddev^2` = 0.0

   override def init(r:Raster,re:Option[RasterExtent]) = {
     super.init(r,re)  
     val h = FastMapHistogram.fromRaster(r)
     val Statistics(m,_,_,s,_,_) = h.generateStatistics
     mean = m
     `stddev^2` = s*s
    }

    def calc(r:Raster,cursor:Cursor) = {
      var z = 0.0
      var w = 0
      var base = 0.0

      cursor.allCells.foreach { (x,y) =>
        if(x == cursor.col && y == cursor.row) {
          base = r.getDouble(x,y)-mean
        } else {
          z += r.getDouble(x,y)-mean
          w += 1
        }
                             }

      data.setDouble(cursor.col,cursor.row,(base / `stddev^2` * z) / w)
    }
  }
})

// Scalar version:

/** Calculates global spatial autocorrelation of a raster based on the similarity to
 * neighboring values.
 *
 * The resulting statistic is such that the more positive the number, the greater
 * the similarity of values in the raster, and the more negative the number,
 * the more dissimilar the raster values are.
 *
 * @param       r         Raster to perform the operation on.
 * @param       n         Neighborhood to use in this focal operation.
 */
case class ScalarMoransI(r:Op[Raster],n:Op[Neighborhood]) extends FocalOp(r,n)({
  (r,n) => new CursorCalculation[Double] with Initialization {
    var mean:Double = 0
    var `stddev^2`:Double = 0

    var count:Double = 0.0
    var ws:Int = 0

    def init(r:Raster, reOpt:Option[RasterExtent]) = {
      val h = FastMapHistogram.fromRaster(r)
      val Statistics(m,_,_,s,_,_) = h.generateStatistics
      mean = m
      `stddev^2` = s*s
    }

    def calc(r:Raster,cursor:Cursor) = {
      var base = r.getDouble(cursor.col,cursor.row) - mean
      var z = -base

      cursor.allCells.foreach { (x,y) => z += r.getDouble(x,y) - mean; ws += 1 }

      count += base / `stddev^2` * z
      ws -= 1 // for focus     
    }

    def getResult = count / ws
  }
})
