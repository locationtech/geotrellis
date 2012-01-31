package trellis

import scala.math.{min, max}

//?TODO: remove rows & cols from IntRaster constructor
object IntRaster {
  def apply(data:RasterData, rows:Int, cols:Int, rasterExtent:RasterExtent):IntRaster = {
    new IntRaster(data, rows, cols, rasterExtent, "")
  }

  def apply(data:RasterData, rasterExtent:RasterExtent) = {
    new IntRaster(data, rasterExtent.rows, rasterExtent.cols, rasterExtent, "")
  }

  def apply(data:RasterData, rows:Int, cols:Int,
            rasterExtent:RasterExtent, name:String) = {
    new IntRaster(data, rows, cols, rasterExtent, name)
  } 
   
  def apply(array:Array[Int], rows:Int, cols:Int, rasterExtent:RasterExtent, name:String = ""):IntRaster = {
    new IntRaster(ArrayRasterData(array), rows, cols, rasterExtent, name)
  }

  def apply(array:Array[Int], rasterExtent:RasterExtent):IntRaster = {
    IntRaster(ArrayRasterData(array), rasterExtent)
  }

  def createEmpty(geo:RasterExtent) = {
    val size = geo.rows * geo.cols
    val data = Array.fill[Int](size)(NODATA)
    IntRaster(ArrayRasterData(data), geo.rows, geo.cols, geo, "")
  }
}

/**
 * 
 */
class IntRaster(val data:RasterData, val rows:Int, val cols:Int, val rasterExtent:RasterExtent,
                val name:String) extends Serializable {

  override def toString = "IntRaster(%s, %s, %s, %s, %s)" format (data, rows, cols, rasterExtent, name)

  override def equals(other:Any) = other match {
    case r:IntRaster => data == r.data && rows == r.rows && cols == r.cols && rasterExtent == r.rasterExtent
    case _ => false
  }
 
  def length = this.rows * this.cols

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
    * Test [[trellis.RasterExtent]] of other raster w/ our own geographic attributes.
    */
  def compare(other:IntRaster) = this.rasterExtent.compare(other.rasterExtent)

  ///**
  //  * Test other raster for equality.
  //  */
  //override def equals(other:Any): Boolean = other match {
  //  case r:IntRaster => {
  //    if (r == null) return false
  //    if (rows != r.rows || this.cols != r.cols) return false
  //    if (rasterExtent != r.rasterExtent) return false
  //
  //    var i = 0
  //    val limit = length
  //    while (i < limit) {
  //      if (data(i) != r.data(i)) return false
  //      i += 1
  //    }
  //
  //    true
  //  }
  //  case _ => false
  //}

  /**
    * Clone this raster.
    */
  def copy() = {
    new IntRaster(this.data.copy, this.rows, this.cols, this.rasterExtent, this.name + "_copy")
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

  def foreach(f: Int => Unit):Unit = {
    var i = 0
    while(i < length) {
      f(data(i))
      i += 1
    }
  }

  def map(f:Int => Int) = {
    val data = this.data
    val output = data.copy //Array.ofDim[Int](length)
    var i = 0
    while (i < length) {
      output(i) = f(data(i))
      i += 1
    }
    new IntRaster(output, this.rows, this.cols, this.rasterExtent, this.name + "_map")
  }

  //TODO: optimize (replace length?, don't copy?)
  def combine2(r2:IntRaster)(f:(Int,Int) => Int) = {
    val data = this.data
    val data2 = r2.data
    val output = data.copy // Array.ofDim[Int](length)
    var i = 0
    while (i < length) {
      output(i) = f(data(i), data2(i))
      i += 1
    }
    new IntRaster(output, this.rows, this.cols, this.rasterExtent, this.name + "_map")
  }

  //def combine2IfSet(r2:IntRaster)(f:(Int,Int) => Int)  = {
  //  val data = this.data
  //  val data2 = r2.data
  //  val output = data.copy
  //  var i = 0
  //  while(i < length) {
  //    val v1 = data(i)
  //    val v2 = data2(i)
  //    output(i) = if (v1 == NODATA || v2 == NODATA) {
  //      if (v1 == NODATA) v2 else v1
  //    } else {
  //      f(data(i),data2(i))
  //    }
  //    i += 1
  //  } 
  //  new IntRaster(output, this.rows, this.cols, this.rasterExtent, this.name + "_map")
  //}
  
  def normalize(zmin:Int, zmax:Int, gmin:Int, gmax:Int) = {
    val grange = gmax - gmin
    val zrange = zmax - zmin
    if (zrange <= 0) {
      copy()
    } else {
      mapIfSet(z => (z - zmin) * (grange / zrange) + gmin)
    }
  }

  def mapIfSet(f:Int => Int) = {
    val data = this.data
    val data2 = this.data.copy
  
    var i = 0
    while (i < length) {
      val z = data(i)
      if (z != NODATA) data2(i) = f(z)
      i += 1
    }
    new IntRaster(data2, this.rows, this.cols, this.rasterExtent, this.name + "_map")
  }
}
