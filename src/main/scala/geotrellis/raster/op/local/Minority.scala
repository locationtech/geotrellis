package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.source._

import scalaxy.loops._
import scala.collection.mutable

object Minority {
  def apply(rs:Op[Raster]*):Op[Raster] =
    apply(0,rs)

  def apply(rs:Seq[Op[Raster]])(implicit d:DI):Op[Raster] =
    apply(0,rs)

  def apply(level:Op[Int],rs:Op[Raster]*):Op[Raster] =
    apply(level,rs)

  def apply(level:Op[Int],rs:Seq[Op[Raster]])(implicit d:DI):Op[Raster] =
    (level,logic.Collect(rs)).map { (level,rs) =>
      if(Set(rs.map(_.rasterExtent)).size != 1) {
        val rasterExtents = rs.map(_.rasterExtent).toSeq
        throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
          s"$rasterExtents are not all equal")
      }

      val layerCount = rs.length
      if(layerCount == 0) {
        sys.error(s"Can't compute majority of empty sequence")
      } else {
        val newRasterType = rs.map(_.rasterType).reduce(_.union(_))
        val re = rs(0).rasterExtent
        val cols = re.cols
        val rows = re.rows
        val data = RasterData.allocByType(newRasterType,cols,rows)

        if(newRasterType.isDouble) {
          val counts = mutable.Map[Double,Int]()

          for(col <- 0 until cols) {
            for(row <- 0 until rows) {
              counts.clear
              for(r <- rs) {
                val v = r.getDouble(col,row)
                if(!isNaN(v)) {
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
                  .sortBy { k => -counts(k) }
                  .toList
              val len = sorted.length - 1
              val m =
                if(len >= level) { sorted(len-level) }
                else { Double.NaN }
              data.setDouble(col,row, m)
            }
          }
        } else {
          val counts = mutable.Map[Int,Int]()

          for(col <- 0 until cols) {
            for(row <- 0 until rows) {
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
                  .sortBy { k => -counts(k) }
                  .toList
              val len = sorted.length - 1
              val m =
                if(len >= level) { sorted(len-level) }
                else { NODATA }
              data.set(col,row, m)
            }
          }
        }
        ArrayRaster(data,re)
      }
    }
    .withName("Minority")
}

trait MinorityOpMethods[+Repr <: RasterDataSource] { self: Repr =>
  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rss:Seq[RasterDS]):RasterDataSource = 
    combineOp(rss)(Minority(_))

  /** Assigns to each cell the value within the given rasters that is the least numerous. */
  def localMinority(rss:RasterDS*)(implicit d:DI):RasterDataSource = 
    localMinority(rss)

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n:Int,rss:Seq[RasterDS]):RasterDataSource = 
    combineOp(rss)(Minority(n,_))

  /** Assigns to each cell the value within the given rasters that is the nth least numerous. */
  def localMinority(n:Int,rss:RasterDS*)(implicit d:DI):RasterDataSource = 
    localMinority(n,rss)
}
