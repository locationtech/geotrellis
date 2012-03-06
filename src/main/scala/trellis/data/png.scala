package geotrellis.data

import java.io.File
import java.nio.ByteBuffer

import geotrellis.data.png.PNGWriter;
import geotrellis.IntRaster

/**
 * Create a PNG image from a [[geotrellis.core.raster.Raster]] 
 */
case class PNGRenderer(raster:IntRaster, path:String, colorMap:Int=>Int, bg:Int,
                       transparent:Boolean) {
  val cols = raster.cols
  val rows = raster.rows

  def getRawBytes() = {
    val data = raster.data

    val bb = ByteBuffer.allocate(cols * rows * 3)
    var i = 0
    var y = 0
    var x = 0
    while (i < rows) {
      y = rows - i - 1
      x = 0
      while (x < cols) {
        val z = colorMap(data(y * cols + x))
        // toByte automatically takes care of the "z & 0xFF"" for us.
        bb.put((z >> 16).toByte)
        bb.put((z >> 8).toByte)
        bb.put(z.toByte)
        x += 1
      }
      i += 1
    }
    bb
  }

  def render() = {
    PNGWriter.makeByteArray(getRawBytes(), rows, cols, bg, transparent)
  }

  def write() {
    PNGWriter.write(new File(path), getRawBytes(), rows, cols, bg, transparent)
  }
}

/**
 * Create a PNG image from a [[geotrellis.core.raster.Raster]] 
 */
case class PNGWriterRGB2(raster:IntRaster, path:String, bg:Int, transparent:Boolean) {
  val cols = raster.cols
  val rows = raster.rows

  def getRawBytes() = {
    val data = raster.data

    val bb = ByteBuffer.allocate(cols * rows * 3)
    var i = 0
    var y = 0
    var x = 0
    while (i < rows) {
      y = rows - i - 1
      x = 0
      while (x < cols) {
        val z = data(y * cols + x)
        // toByte automatically takes care of the "z & 0xFF"" for us.
        bb.put((z >> 16).toByte)
        bb.put((z >> 8).toByte)
        bb.put(z.toByte)
        x += 1
      }
      i += 1
    }
    bb
  }

  def render() = {
    PNGWriter.makeByteArray(getRawBytes(), rows, cols, bg, transparent)
  }

  def write() {
    PNGWriter.write(new File(path), getRawBytes(), rows, cols, bg, transparent)
  }
}
