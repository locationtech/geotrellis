package geotrellis

import geotrellis.raster._
import scalaxy.loops._

object RasterSeq {
  implicit def seqToRasterSeq(seq:Seq[Raster]):RasterSeq = RasterSeq(seq)
}

/**
 * Base trait for the Raster data type.
 */
case class RasterSeq(seq:Seq[Raster]) {
  if(seq.length < 2) {
    sys.error(s"Cannot have a raster sequence with less than 2 rasters.")
  }

  if(Set(seq.map(_.rasterExtent)).size != 1) {
    val rasterExtents = seq.map(_.rasterExtent).toSeq
    throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
      s"$rasterExtents are not all equal")
  }

  val head = seq.head
  val rasterExtent = head.rasterExtent
  val (cols,rows) = (rasterExtent.cols,rasterExtent.rows)
  val layerCount = seq.length
  val rasterType = seq.map(_.rasterType).reduce(_.union(_))


  def combine(f:Seq[Int] => Int):Raster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        data.set(col,row,f(seq.map(_.get(col,row))))
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def reduce(f:(Int,Int)=>Int):Raster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        var v = head.get(col,row)
        for(i <- 1 until layerCount optimized) {
          v = f(v,seq(i).get(col,row))
        }

        data.set(col,row,v)
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def combineDouble(f:Seq[Double] => Double):Raster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        data.setDouble(col,row,f(seq.map(_.getDouble(col,row))))
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def reduceDouble(f:(Double,Double)=>Double):Raster = {
    val data = RasterData.allocByType(rasterType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        var v = head.getDouble(col,row)
        for(i <- 1 until layerCount optimized) {
          v = f(v,seq(i).getDouble(col,row))
        }

        data.setDouble(col,row,v)
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def dualCombine(f:Seq[Int] => Int)(g:Seq[Double] => Double) =
    seq.map(_.isFloat) find(b=>b) match {
      case Some(_) => combineDouble(g)
      case _ => combine(f)
    }

  def dualReduce(f:(Int,Int)=>Int)(g:(Double,Double)=>Double) = 
    seq.map(_.isFloat) find(b=>b) match {
      case Some(_) => reduceDouble(g)
      case _ => reduce(f)
    }
}
