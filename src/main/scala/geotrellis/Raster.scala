package geotrellis

import geotrellis.raster.TiledRasterData
import geotrellis.operation._

import scala.math.{min, max}

object Raster {
  def apply(arr:Array[Int], re:RasterExtent):Raster = 
    Raster(IntArrayRasterData(arr), re)

  def empty(re:RasterExtent):Raster = 
    Raster(IntArrayRasterData.empty(re.rows * re.cols), re)
}

/**
 * 
 */
case class Raster (data:RasterData, rasterExtent:RasterExtent) {

  def cols = rasterExtent.cols
  def rows = rasterExtent.rows
  def length = rasterExtent.size

  def toArray = data.asArray.toArray

  /**
   * Get value at given coordinates.
   */
  def get(col:Int, row:Int):Int = data.get(col, row, cols)

  /**
   * Return tuple of highest and lowest value in raster.
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

  def foreach(f: Int => Unit):Unit = data.foreach(f)

  def map(f:Int => Int) = Raster(data.map(f),rasterExtent)

  def fold[A](a: =>A)(f:(A,Int) => A):A = data.fold(a)(f)

  def combine2(r2:Raster)(f:(Int,Int) => Int) = {
    Raster(data.combine2(r2.data)(f), rasterExtent)
  }

  def normalize(zmin:Int, zmax:Int, gmin:Int, gmax:Int): Raster = {
    val dg = gmax - gmin
    val dz = zmax - zmin
    if (dz > 0) mapIfSet(z => ((z - zmin) * dg) / dz + gmin) else copy()
  }

  def mapIfSet(f:Int => Int) = Raster(data.mapIfSet(f), rasterExtent)

  def force = data match {
    case a:ArrayRasterData => Raster(a.force, rasterExtent)
    case _ => sys.error("force called on non-array raster data")
  }

  def defer = Raster(data.defer, rasterExtent)

  def getTiles:Array[Raster] = data match {
    case t:TiledRasterData => t.getTiles(rasterExtent)
    case _ => Array(this)
  }

  def getTileList:List[Raster] = data match {
    case t:TiledRasterData => t.getTileList(rasterExtent)
    case _ => this :: Nil
  }

  def getTileOpList:List[Op[Raster]] = data match {
    case t:TiledRasterData => t.getTileOpList(rasterExtent)
    case _ => Literal(this) :: Nil
  }
}
