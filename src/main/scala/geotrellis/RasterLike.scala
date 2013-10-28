package geotrellis

import geotrellis.raster._
import scalaxy.loops._

trait RasterLike {
  val rasterExtent:RasterExtent

  lazy val cols = rasterExtent.cols
  lazy val rows = rasterExtent.rows

  val rasterType:RasterType
  def isFloat:Boolean = rasterType.float // TODO: yup.

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
