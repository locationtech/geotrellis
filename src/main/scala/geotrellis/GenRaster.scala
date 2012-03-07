package geotrellis

import scala.math.{min, max}

import Predef.{any2stringadd => _, _}
import scala.{specialized => spec}
import com.azavea.math.{Numeric => Num}
import com.azavea.math.FastImplicits._

/**
 * Used to associate a type parameter T with a particular value which geotrellis
 * uses as the "nodata value".
 */
trait NoData[T] {
  def value:T
}

/**
 * Companion object containing some no data values for types geotrellis supports.
 */
object NoData {
  implicit object IntNoData extends NoData[Int] { final def value = Int.MinValue }
  implicit object LongNoData extends NoData[Long] { final def value = Long.MinValue }
  implicit object FloatNoData extends NoData[Float] { final def value = Float.NaN }
  implicit object DoubleNoData extends NoData[Double] { final def value = Double.NaN }
}

object GenRaster {
  def apply[@spec T:Num:Manifest:NoData](data:GenRasterData[T], rasterExtent:RasterExtent, name:String) = {
    new GenRaster(data, rasterExtent, name)
  } 

  def apply[@spec T:Num:Manifest:NoData](data:GenRasterData[T], rasterExtent:RasterExtent) = {
    new GenRaster(data, rasterExtent, "")
  }
   
  def apply[@spec T:Num:Manifest:NoData](array:Array[T], rasterExtent:RasterExtent, name:String) = {
    new GenRaster(ArrayGenRasterData(array), rasterExtent, name)
  }

  def apply[@spec T:Num:Manifest:NoData](array:Array[T], rasterExtent:RasterExtent) = {
    new GenRaster(ArrayGenRasterData(array), rasterExtent, "")
  }

  def createEmpty[@spec T:Num:Manifest:NoData](re:RasterExtent) = {
    val nodata = implicitly[NoData[T]].value
    val size = re.rows * re.cols
    val data = Array.fill[T](size)(nodata)
    new GenRaster(ArrayGenRasterData(data), re, "")
  }
}

/**
 * 
 */
final class GenRaster[@spec T:Num:Manifest:NoData](val data:GenRasterData[T],
                                                val rasterExtent:RasterExtent,
                                                val name:String) extends Serializable {

  final val nodata = implicitly[NoData[T]].value

  override def toString = "GenRaster(%s, %s, %s, %s, %s)" format (data, rasterExtent, name)

  override def equals(other:Any) = other match {
    case r:GenRaster[_] => data == r.data && rasterExtent == r.rasterExtent
    case _ => false
  }

  def cols = rasterExtent.cols
  def rows = rasterExtent.rows
  def length = rasterExtent.size

  /**
    * Get value at given coordinates.
    */
  def get(col:Int, row:Int) = data(row * cols + col)

  /**
    * Set value at given coordinates.
    */
  def set(col:Int, row:Int, value:T) {
    data(row * cols + col) = value
  }

  /**
    * Return tuple of highest and lowest value in raster.
    */
  def findMinMax = {
    var zmin = data(0)
    var zmax = data(0)

    var i = 1
    while (i < length) {
      val z = data(i)
      if (z != nodata) {
        zmin = numeric.min(zmin, z)
        zmax = numeric.max(zmax, z)
      }
      i += 1
    }
    (zmin, zmax)
  }      

  /**
    * Return data in this raster as a one-dimensional array (row by row).
    */
  def asArray = data.asArray

  /**
    * Test [[geotrellis.RasterExtent]] of other raster w/ our own geographic attributes.
    */
  def compare(other:GenRaster[T]) = rasterExtent.compare(other.rasterExtent)

  /**
    * Clone this raster.
    */
  def copy() = new GenRaster(data.copy, rasterExtent, name + "_copy")

  /**
    * Return ascii art of this raster.
    */
  def asciiDraw() = {
    var s = "";
    for (row <- 0 until rows) {
      for (col <- 0 until cols) {
        val z = data(row * cols + col)
        if (z == nodata) {
          s += ".."
        } else {
          s += "%02X".format(z.toInt)
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
        val z = data(row * cols + col)
        if (z == nodata) {
          s += ".."
        } else {
          s += "%02X".format(z.toInt)
        }
      }
      s += "\n"
    }
    s
  }

  def foreach(f:T => Unit):Unit = {
    var i = 0
    while(i < length) {
      f(data(i))
      i += 1
    }
  }

  /**
   */
  def map(f:T => T) = {
    val d = data
    val output = d.copy
    var i = 0
    while (i < length) {
      output(i) = f(d(i))
      i += 1
    }
    new GenRaster(output, rasterExtent, name + "_map")
  }

  //TODO: optimize (replace length?, don't copy?)
  def combine2(r2:GenRaster[T])(f:(T, T) => T) = {
    val d = data
    val d2 = r2.data
    val output = d.copy
    var i = 0
    while (i < length) {
      output(i) = f(d(i), d2(i))
      i += 1
    }
    new GenRaster(output, rasterExtent, name + "_map")
  }

  /**
   */
  def normalize(gmin:T, gmax:T):GenRaster[T] = {
    val (zmin, zmax) = findMinMax
    normalize(zmin, zmax, gmin, gmax)
  }

  /**
   */
  def normalize(zmin:T, zmax:T, gmin:T, gmax:T):GenRaster[T] = {
    val grange = gmax - gmin
    val zrange = zmax - zmin
    if (zrange <= numeric.zero) {
      copy()
    } else {
      mapIfSet(z => (z - zmin) * (grange / zrange) + gmin)
    }
  }

  /**
   * Convenience version of 'map' which skips nodata values. For instance:
   *
   *  r.mapIfSet(x => x + 2)
   *
   * is equivalent to:
   *
   *  r.map(x => if (x == nodata) x else x + 2) 
   */
  def mapIfSet(f:T => T) = {
    val d = data
    val d2 = data.copy
    var i = 0
    while (i < length) {
      val z = data(i)
      if (z != nodata) d2(i) = f(z)
      i += 1
    }
    new GenRaster(d2, rasterExtent, name + "_map")
  }
}
