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
trait Raster {
  val rasterExtent:RasterExtent
  lazy val cols = rasterExtent.cols
  lazy val rows = rasterExtent.rows

  /** This function will force any deferred operations to happen
   *  at the time it is called. This is necessary so that lazy maps
   *  don't get passed between machines.
   */
//  def force():Raster

  val rasterType:RasterType
  def isFloat:Boolean = rasterType.float

  /**
   * Get value at given coordinates.
   */
  def get(col:Int, row:Int):Int

  /**
   * Get value at given coordinates.
   */
  def getDouble(col:Int, row:Int):Double

  def toArrayRaster():ArrayRaster
  def toArray():Array[Int]
  def toArrayDouble():Array[Double]

  /**
   * Clone this raster.
   */
//  def copy():Raster
  def convert(typ:RasterType):Raster

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

    val rasters = this +: rs
    val layerCount = rasters.length
    if(layerCount == 0) {
      this
    } else {
      val newRasterType = rasters.map(_.rasterType).reduce(_.union(_))
      val data = RasterData.allocByType(newRasterType,cols,rows)
      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          var v = get(col,row)
          for(i <- 1 until layerCount optimized) {
            v = f(v,rasters(i).get(col,row))
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

    val rasters = this +: rs
    val layerCount = rasters.length
    if(layerCount == 0) {
      this
    } else {
      val newRasterType = rasters.map(_.rasterType).reduce(_.union(_))
      val data = RasterData.allocByType(newRasterType,cols,rows)
      for(col <- 0 until cols optimized) {
        for(row <- 0 until rows optimized) {
          var v = getDouble(col,row)
          for(i <- 1 until layerCount optimized) {
            v = f(v,rasters(i).getDouble(col,row))
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

  /**
   * Normalizes the values of this raster, given the current min and max, to a new min and max.
   * 
   *   @param oldMin    Old mininum value
   *   @param oldMax    Old maximum value
   *   @param newMin     New minimum value
   *   @param newMax     New maximum value
   */
  def normalize(oldMin:Int, oldMax:Int, newMin:Int, newMax:Int): Raster = {
    val dnew = newMax - newMin
    val dold = oldMax - oldMin
    if(dold <= 0 || dnew <= 0) { sys.error(s"Invalid parameters: $oldMin,$oldMax,$newMin,$newMax") }
    mapIfSet(z => ( ((z - oldMin) / dold) * dnew ) + newMin)
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
  def asciiDraw():String = { 
    val sb = new StringBuilder
    for(row <- 0 until rows) {
      for(col <- 0 until cols) {
        val v = get(col,row)
        val s = if(v == NODATA) {
          "ND"
        } else {
          s"$v"
        }
        val pad = " " * math.max(6 - s.length,0) 
        sb.append(s"$pad$s")
      }
      sb += '\n'
    }      
    sb += '\n'
    sb.toString
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
