package geotrellis

import scala.math.{min, max}

//?TODO: remove rows & cols from IntRaster constructor
object IntRaster {
  def apply(data:RasterData, rasterExtent:RasterExtent):IntRaster = {
    new IntRaster(data, rasterExtent, "")
  }

  def apply(data:RasterData, rasterExtent:RasterExtent, name:String) = {
    new IntRaster(data, rasterExtent, name)
  } 
   
  def apply(array:Array[Int], rasterExtent:RasterExtent):IntRaster = {
    new IntRaster(ArrayRasterData(array), rasterExtent, "")
  }

  def apply(array:Array[Int], rasterExtent:RasterExtent, name:String):IntRaster = {
    new IntRaster(ArrayRasterData(array), rasterExtent, name)
  }

  def createEmpty(geo:RasterExtent) = {
    val size = geo.rows * geo.cols
    val data = Array.fill[Int](size)(NODATA)
    IntRaster(ArrayRasterData(data), geo, "")
  }
}

/**
 * 
 */
class IntRaster(val data:RasterData, val rasterExtent:RasterExtent,
                val name:String) extends Serializable {

  override def toString = "IntRaster(%s, %s, %s, %s, %s)" format (data, rows, cols, rasterExtent, name)

  override def equals(other:Any) = other match {
    case r:IntRaster => data == r.data && rows == r.rows && cols == r.cols && rasterExtent == r.rasterExtent
    case _ => false
  }

  def cols = rasterExtent.cols

  def rows = rasterExtent.rows
 
  def length = rasterExtent.size

  /**
    * Get value at given coordinates.
    */
  def get(col:Int, row:Int) = this.data(row * this.cols + col)

  /**
    * Set value at given coordinates.
    */
  def set(col:Int, row:Int, value:Int) {
    this.data(row * this.cols + col) = value
  }

  /**
    * Return tuple of highest and lowest value in raster.
    */
  def findMinMax = {
    var zmin = Int.MaxValue
    var zmax = Int.MinValue

    var i = 0
    while (i < length) {
      val z = this.data(i)
      if (z != NODATA) {
        zmin = min(zmin, z)
        zmax = max(zmax, z)
      }
      i += 1
    }
    (zmin, zmax)
  }      

  /**
    * Return data in this raster as a one-dimensional array (row by row).
    */
  def asArray = this.data.asArray

  /**
    * Test [[geotrellis.RasterExtent]] of other raster w/ our own geographic attributes.
    */
  def compare(other:IntRaster) = this.rasterExtent.compare(other.rasterExtent)

  /**
    * Clone this raster.
    */
  def copy() = {
    new IntRaster(data.copy, rasterExtent, name + "_copy")
  }

  /**
    * Return ascii art of this raster.
    */
  def asciiDraw() = {
    var s = "";
    for (row <- 0 until this.rows) {
      for (col <- 0 until this.cols) {
        val z = this.data(row * this.cols + col)
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
        val z = this.data(row * this.cols + col)
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

  def map(f:Int => Int) = IntRaster(data.map(f), rasterExtent)

  //TODO: implement RasterData.ofDim and use that instead
  def combine2(r2:IntRaster)(f:(Int,Int) => Int) = {
    val data1 = this.data
    val data2 = r2.data
    val output = data1.copy // RasterData.ofDim[Int](length)
    var i = 0
    val len = length
    while (i < len) {
      output(i) = f(data1(i), data2(i))
      i += 1
    }
    IntRaster(output, rasterExtent)
  }

  def normalize(zmin:Int, zmax:Int, gmin:Int, gmax:Int) = {
    val grange = gmax - gmin
    val zrange = zmax - zmin
    if (zrange <= 0) {
      copy()
    } else {
      mapIfSet(z => (z - zmin) * (grange / zrange) + gmin)
    }
  }

  def mapIfSet(f:Int => Int) = IntRaster(data.mapIfSet(f), rasterExtent)
}
