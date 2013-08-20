package geotrellis

import geotrellis.raster.TiledRasterData
import geotrellis._
import geotrellis.util.Filesystem
import geotrellis.feature.Polygon
import java.io.File
import scala.math.{min, max}
import geotrellis.util.Filesystem
import geotrellis.raster._

object Raster {
  def apply(arr:Array[Int], re:RasterExtent):Raster = 
    Raster(IntArrayRasterData(arr, re.cols, re.rows), re)

  def apply(arr:Array[Double], re:RasterExtent):Raster = 
    Raster(DoubleArrayRasterData(arr, re.cols, re.rows), re)

  def empty(re:RasterExtent):Raster = 
    Raster(IntArrayRasterData.empty(re.cols, re.rows), re)
}

/**
 * 
 */
case class Raster (data:RasterData, rasterExtent:RasterExtent) {

  def cols = rasterExtent.cols
  def rows = rasterExtent.rows
  def length = rasterExtent.size

  def isFloat = data.getType.float

  /**
    * Returns true if the underlying data is tiled. 
    */
  def isTiled:Boolean = data.isTiled

  /**
    * Returns true if the underlying data is being lazily evaluated.
    */
  def isLazy:Boolean = data.isLazy

  def toArray = data.asArray.getOrElse(sys.error("argh")).toArray
  def toArrayDouble = data.asArray.getOrElse(sys.error("argh")).toArrayDouble

  /**
   * Get value at given coordinates.
   */
  def get(col:Int, row:Int):Int = data.get(col, row)

  /**
   * Get value at given coordinates.
   */
  def getDouble(col:Int, row:Int):Double = data.getDouble(col, row)

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

    data.foreach {
      z => if (z != NODATA) {
        zmin = min(zmin, z)
        zmax = max(zmax, z)
      }
    }

    if(zmin == Int.MaxValue) { zmin = NODATA }
    (zmin, zmax)
  } 

  /**
   * Return tuple of highest and lowest value in raster.
   *
   * @note   Currently does not support double valued raster data types
   *         (TypeFloat,TypeDouble). Calling findMinMax on rasters of those
   *         types will give the integer min and max of the rounded values of
   *         their cells.
   */
  def findMinMaxDouble = {
    var zmin = Double.NaN
    var zmax = Double.NaN

    data.foreachDouble {
      z => if (!java.lang.Double.isNaN(z)) {
        if(java.lang.Double.isNaN(zmin)) {
          zmin = z
          zmax = z
        } else {
          zmin = min(zmin, z)
          zmax = max(zmax, z)
        }
      }
    }

    (zmin, zmax)
  } 

  /**
   * Test [[geotrellis.RasterExtent]] of other raster w/ our own geographic
   *attributes.
   */
  def compare(other:Raster) = this.rasterExtent.compare(other.rasterExtent)

  /**
   * Clone this raster.
   */
  def copy() = Raster(data.copy, rasterExtent)
  def convert(typ:RasterType) = Raster(data.convert(typ), rasterExtent)

  /**
   * Return ascii art of this raster.
   */
  def asciiDraw() = { 
    var s = "";
    for (row <- 0 until this.rows) {
      for (col <- 0 until this.cols) {
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

  def dualForeach(f:Int => Unit)(g:Double => Unit):Unit =
    if (isFloat) foreachDouble(g) else foreach(f)

  def dualMap(f:Int => Int)(g:Double => Double) =
    if (isFloat) mapDouble(g) else map(f)

  def dualMapIfSet(f:Int => Int)(g:Double => Double) =
    if (isFloat) mapIfSetDouble(g) else mapIfSet(f)

  def dualCombine(r2:Raster)(f:(Int, Int) => Int)(g:(Double, Double) => Double) =
    if (isFloat || r2.isFloat) combineDouble(r2)(g) else combine(r2)(f)

  def foreach(f:Int => Unit):Unit = data.foreach(f)

  def map(f:Int => Int) = Raster(data.map(f),rasterExtent)

  def mapIfSet(f:Int => Int) = Raster(data.mapIfSet(f), rasterExtent)

  def combine(r2:Raster)(f:(Int, Int) => Int) = {
    Raster(data.combine(r2.data)(f), rasterExtent)
  }

  def foreachDouble(f:Double => Unit):Unit = data.foreachDouble(f)

  def mapDouble(f:Double => Double) = Raster(data.mapDouble(f), rasterExtent)

  def mapIfSetDouble(f:Double => Double) = Raster(data.mapIfSetDouble(f), rasterExtent)

  def combineDouble(r2:Raster)(f:(Double, Double) => Double) = {
    Raster(data.combineDouble(r2.data)(f), rasterExtent)
  }

  def normalize(zmin:Int, zmax:Int, gmin:Int, gmax:Int): Raster = {
    val dg = gmax - gmin
    val dz = zmax - zmin
    if (dz > 0) mapIfSet(z => ((z - zmin) * dg) / dz + gmin) else copy()
  }

  def force() = {
    val opt = data.force.map(d => Raster(d, rasterExtent))
    opt.getOrElse(sys.error("force called on non-array raster data"))
  }

  def defer() = data.asArray.map(d => Raster(LazyArrayWrapper(d), rasterExtent)).getOrElse(this)

  def getTiles():List[Raster] = data match {
    case t:TiledRasterData => t.getTiles(rasterExtent)
    case _ => List(this)
  }

  def getTileOpList():List[Op[Raster]] = data match {
    case t:TiledRasterData => t.getTileOpList(rasterExtent)
    case _ => Literal(this) :: Nil
  }

  //TODO: update Literal() case to return empty list if necessary
  def getTileOpList(clipExtent:Polygon[_]):List[Op[Raster]] = data match {
    case t:TiledRasterData => t.getTileOpList(rasterExtent, clipExtent)
    case _ => Literal(this) :: Nil
  }
}
