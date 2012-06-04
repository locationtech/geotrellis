package geotrellis

import scala.math.{min, max}

object Raster {
  def apply(arr:Array[Int], re:RasterExtent):Raster = {
    new Raster(IntArrayRasterData(arr), re)
  }

  def empty(re:RasterExtent) = {
    Raster(IntArrayRasterData.empty(re.rows * re.cols), re)
  }
}

/**
 * 
 */
case class Raster(data:RasterData, rasterExtent:RasterExtent) {

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
   * Test [[geotrellis.RasterExtent]] of other raster w/ our own geographic
   *attributes.
   */
  def compare(other:Raster) = this.rasterExtent.compare(other.rasterExtent)

  /**
   * Clone this raster.
   */
  def copy() = new Raster(data.copy, rasterExtent)

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

  def map(f:Int => Int) = Raster(data.map(f), rasterExtent)

  def combine2(r2:Raster)(f:(Int,Int) => Int) = {
    Raster(data.combine2(r2.data)(f), rasterExtent)
  }

  def normalize(zmin:Int, zmax:Int, gmin:Int, gmax:Int): Raster = {
    val dg = gmax - gmin
    val dz = zmax - zmin
    if (dz > 0) mapIfSet(z => ((z - zmin) * dg) / dz + gmin) else copy()
  }

  def mapIfSet(f:Int => Int) = Raster(data.mapIfSet(f), rasterExtent)

  def force = Raster(data.force, rasterExtent)
  def defer = Raster(data.defer, rasterExtent)
}
