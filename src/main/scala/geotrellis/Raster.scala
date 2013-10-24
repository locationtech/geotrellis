package geotrellis

import geotrellis.raster._
import scalaxy.loops._

object Raster {
  def apply(arr:RasterData, re:RasterExtent):Raster = 
    ArrayRaster(arr,re)

  def apply(arr:Array[Int], re:RasterExtent):Raster = 
    ArrayRaster(IntArrayRasterData(arr, re.cols, re.rows), re)

  def apply(arr:Array[Double], re:RasterExtent):Raster = 
    ArrayRaster(DoubleArrayRasterData(arr, re.cols, re.rows), re)

  def empty(re:RasterExtent):Raster = 
    ArrayRaster(IntArrayRasterData.empty(re.cols, re.rows), re)
}

/**
 * Base trait for the Raster data type.
 */
trait Raster extends RasterLike {
  /** This function will force any deferred operations to happen
   *  at the time it is called. This is necessary so that lazy maps
   *  don't get passed between machines.
   */
  def force():Raster

  def toArray:Array[Int]
  def toArrayDouble:Array[Double]

  /**
   * Clone this raster.
   */
  def copy():Raster
  def convert(typ:RasterType):Raster

  def map(f:Int => Int):Raster
  def combine(r2:Raster)(f:(Int, Int) => Int):Raster

  def mapDouble(f:Double => Double):Raster
  def combineDouble(r2:Raster)(f:(Double, Double) => Double):Raster


  def combine(rs:Seq[Raster])(f:Seq[Int] => Int):Raster = {
    if(Set(rs.map(_.rasterExtent)).size != 1) {
      val rasterExtents = rs.map(_.rasterExtent).toSeq
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtents are not all equal")
    }
    val rasters = this +: rs
    val newRasterType = rasters.map(_.rasterType).reduce(_.union(_))
    val data = RasterData.allocByType(newRasterType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        data.set(col,row,f(rasters.map(_.get(col,row))))
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def combine(rs:Raster*)(f:Seq[Int] => Int)(implicit d:DI):Raster = 
    combine(rs)(f)

  def reduce(rs:Seq[Raster])(f:(Int,Int)=>Int):Raster = {
    if(Set(rs.map(_.rasterExtent)).size != 1) {
      val rasterExtents = rs.map(_.rasterExtent).toSeq
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtents are not all equal")
    }

    val layerCount = rs.length
    if(layerCount == 0) {
      this
    } else {
      val newRasterType = rs.map(_.rasterType).reduce(_.union(_))
      val data = RasterData.allocByType(newRasterType,cols,rows)
      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          var v = get(col,row)
          for(i <- 1 until layerCount optimized) {
            v = f(v,rs(i).get(col,row))
          }

          data.set(col,row,v)
        }
      }
      ArrayRaster(data,rasterExtent)
    }
  }

  def combineDouble(rs:Seq[Raster])(f:Seq[Double] => Double):Raster = {
    if(Set(rs.map(_.rasterExtent)).size != 1) {
      val rasterExtents = rs.map(_.rasterExtent).toSeq
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtents are not all equal")
    }
    val rasters = this +: rs
    val newRasterType = rasters.map(_.rasterType).reduce(_.union(_))
    val data = RasterData.allocByType(newRasterType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        data.setDouble(col,row,f(rasters.map(_.getDouble(col,row))))
      }
    }
    ArrayRaster(data,rasterExtent)
  }

  def combineDouble(rs:Raster*)(f:Seq[Double] => Double)(implicit d:DI):Raster = 
    combineDouble(rs)(f)

  def reduceDouble(rs:Seq[Raster])(f:(Double,Double)=>Double):Raster = {
    if(Set(rs.map(_.rasterExtent)).size != 1) {
      val rasterExtents = rs.map(_.rasterExtent).toSeq
      throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
                             s"$rasterExtents are not all equal")
    }

    val layerCount = rs.length
    if(layerCount == 0) {
      this
    } else {
      val newRasterType = rs.map(_.rasterType).reduce(_.union(_))
      val data = RasterData.allocByType(newRasterType,cols,rows)
      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          var v = getDouble(col,row)
          for(i <- 1 until layerCount optimized) {
            v = f(v,rs(i).getDouble(col,row))
          }

          data.setDouble(col,row,v)
        }
      }
      ArrayRaster(data,rasterExtent)
    }
  }

  def mapIfSet(f:Int => Int):Raster =
    map { i =>
      if(i == NODATA) i
      else f(i)
    }

  def mapIfSetDouble(f:Double => Double):Raster = 
    mapDouble { d =>
      if(isNaN(d)) d
      else f(d)
    }

  def dualMap(f:Int => Int)(g:Double => Double) =
    if (isFloat) mapDouble(g) else map(f)

  def dualMapIfSet(f:Int => Int)(g:Double => Double) =
    if (isFloat) mapIfSetDouble(g) else mapIfSet(f)

  def dualCombine(r2:Raster)(f:(Int, Int) => Int)(g:(Double, Double) => Double) =
    if (isFloat || r2.isFloat) combineDouble(r2)(g) else combine(r2)(f)

  def dualCombine(rs:Seq[Raster])(f:Seq[Int] => Int)(g:Seq[Double] => Double) =
    (isFloat +: rs.map(_.isFloat)) find(b=>b) match {
      case Some(_) => combineDouble(rs)(g)
      case _ => combine(rs)(f)
    }

  def dualReduce(rs:Seq[Raster])(f:(Int,Int)=>Int)(g:(Double,Double)=>Double) = 
    (isFloat +: rs.map(_.isFloat)) find(b=>b) match {
      case Some(_) => reduceDouble(rs)(g)
      case _ => reduce(rs)(f)
    }

  /**
   * Test [[geotrellis.RasterExtent]] of other raster w/ our own geographic
   *attributes.
   */
  def compare(other:Raster) = this.rasterExtent.compare(other.rasterExtent)

  def normalize(zmin:Int, zmax:Int, gmin:Int, gmax:Int): Raster = {
    val dg = gmax - gmin
    val dz = zmax - zmin
    if (dz > 0) mapIfSet(z => ((z - zmin) * dg) / dz + gmin) else copy()
  }
}
