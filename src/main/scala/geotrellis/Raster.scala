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

trait RasterLike {
  val rasterExtent:RasterExtent

  val cols = rasterExtent.cols
  val rows = rasterExtent.rows

  val rasterType:RasterType
  val isFloat:Boolean = rasterType.float // TODO: yup.

  def toArrayRaster:ArrayRaster

  /**
   * Get value at given coordinates.
   */
  def get(col:Int, row:Int):Int

  /**
   * Get value at given coordinates.
   */
  def getDouble(col:Int, row:Int):Double

  def dualForeach(f:Int => Unit)(g:Double => Unit):Unit =
    if (isFloat) foreachDouble(g) else foreach(f)

  def foreach(f:Int=>Unit):Unit =
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        f(get(col,row))
      }
    }

  def foreachDouble(f:Double=>Unit):Unit =
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        f(getDouble(col,row))
      }
    }

  /**
   * Return tuple of highest and lowest value in raster.
   *
   * @note   Currently does not support double valued raster data types
   *         (TypeFloat,TypeDouble). Calling findMinMax on rasters of those
   *         types will give the integer min and max of the rounded values of
   *         their cells.
   */
  def findMinMax = {
    var zmin = Int.MaxValue
    var zmax = Int.MinValue

    foreach { 
      z => if (z != NODATA) {
        zmin = math.min(zmin, z)
        zmax = math.max(zmax, z)
      }
    }

    if(zmin == Int.MaxValue) { zmin = NODATA }
    (zmin, zmax)
  } 

  /**
   * Return tuple of highest and lowest value in raster.
   */
  def findMinMaxDouble = {
    var zmin = Double.NaN
    var zmax = Double.NaN

    foreachDouble {
      z => if (!java.lang.Double.isNaN(z)) {
        if(java.lang.Double.isNaN(zmin)) {
          zmin = z
          zmax = z
        } else {
          zmin = math.min(zmin, z)
          zmax = math.max(zmax, z)
        }
      }
    }

    (zmin, zmax)
  } 

  /**
   * Return ascii art of this raster.
   */
  def asciiDraw() = { 
    var s = "";
    for (row <- 0 until rasterExtent.rows) {
      for (col <- 0 until rasterExtent.cols) {
        val z = this.get(col,row)
        if (z == NODATA) {
          s += ".."
        } else {
          s += "%02X".format(z)
        }
      }
      s += "\n"
    }
    s
  }

  /**
   * Return ascii art of a range from this raster.
   */
  def asciiDrawRange(colMin:Int, colMax:Int, rowMin:Int, rowMax:Int) = {
    var s = "";
    for (row <- rowMin to rowMax) {
      for (col <- colMin to colMax) {
        val z = this.get(row, col)
        if (z == NODATA) {
          s += ".."
        } else {
          s += "%02X".format(z)
        }
      }
      s += "\n"
    }
    s
  }
}

/**
 * 
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

  def combine(rs:Raster*)(f:Seq[Int] => Int)(implicit d:DI):Raster = 
    combine(rs)(f)

  def combineDouble(rs:Raster*)(f:Seq[Double] => Double)(implicit d:DI):Raster = 
    combineDouble(rs)(f)

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
