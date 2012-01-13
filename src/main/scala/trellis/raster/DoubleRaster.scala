package trellis.raster

import java.io.Serializable
import scala.math.{min, max}
import trellis.constant.NODATA
import trellis.RasterExtent

object DoubleRaster {
  def apply(data:Array[Double], rows:Int, cols:Int,
            rasterExtent:RasterExtent) = new DoubleRaster(data, rows, cols, rasterExtent)
  def createEmpty(geo:RasterExtent) = {
    val size = geo.rows * geo.cols
    val data = Array.fill[Double](size)(NODATA)
    new DoubleRaster(data, geo.rows, geo.cols, geo)
  }
}

/**
  * Core data object that represents a raster with double values.
  */
class DoubleRaster(val data:Array[Double], val rows:Int, val cols:Int,
                   val rasterExtent:RasterExtent) extends Serializable {
  val length = this.rows * this.cols

  def get(col:Int, row:Int) = this.data(row * this.cols + col)

  def set(col:Int, row:Int, value:Double) {
    this.data(row * this.cols + col) = value
  }

  def findMinMax = {
    var zMin = Double.MaxValue
    var zMax = Double.MinValue

    var i = 0
    while (i < length) {
      val z = this.data(i)
      if (z != NODATA) {
        zMin = min(zMin, z)
        zMax = max(zMax, z)
      }
      i += 1
    }
    (zMin, zMax)
  }      

  def asArray = this.data

  def compare(other:DoubleRaster) = this.rasterExtent.compare(other.rasterExtent)

  def equal(other:DoubleRaster): Boolean = {
    if (null == other) return false
    if (rows != other.rows) return false
    if (cols != other.cols) return false
    if (rasterExtent != other.rasterExtent) return false

    var i = 0
    val len = data.length
    while (i < len) {
      if (data(i) != other.data(i)) return false
      i += 1
    }

    true
  }

  def copy = {
    new DoubleRaster(this.data.clone, this.rows, this.cols, this.rasterExtent)
  }

  def asciiDraw = {
    var s = "";
    for (row <- 0 until this.rows) {
      for (col <- 0 until this.cols) {
        val z = this.data(row * this.cols + col)
        if (z == NODATA) {
          s += ".."
        } else {
          s += "%08.1f ".format(z)
        }
      }
      s += "\n"
    }
    s
  }
}
