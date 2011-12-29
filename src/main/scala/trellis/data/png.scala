package trellis.data

import java.io.File
import java.nio.ByteBuffer

import trellis.operation.render.png.PNGWriter;
import trellis.raster.IntRaster

/**
  * Create a PNG image from a [[trellis.core.raster.Raster]] 
  */
class PNGWriterRGB(raster:IntRaster, path:String, colorMap:Int=>Int, bg:Int,
                   transparent:Boolean) {
  def getRawBytes = {
    val data = this.raster.data
    val cols = this.raster.cols
    val rows = this.raster.rows

    val bb = ByteBuffer.allocate(cols * rows * 3)
    var i = 0
    while (i < rows) {
      // for some reason PNGWriter is reversed... ugh
      val y = rows - i - 1
      var x = 0
      while (x < cols) {
        val z = colorMap(data(y * cols + x))
        bb.put(((z >> 16) & 0xFF).toByte)
        bb.put(((z >> 8) & 0xFF).toByte)
        bb.put(((z) & 0xFF).toByte)
        x += 1
      }
      i += 1
    }
    bb
  }

  def render = {
    val bb = this.getRawBytes
    PNGWriter.makeByteArray(bb, this.raster.rows, this.raster.cols, this.bg, this.transparent)
  }

  def write {
    val bb = this.getRawBytes
    val f = new File(this.path)
    PNGWriter.write(f, bb, this.raster.rows, this.raster.cols, this.bg, this.transparent)
  }
}

/**
  * Create a PNG image from a [[trellis.core.raster.Raster]] 
  */
class PNGWriterRGB2(raster:IntRaster, path:String, bg:Int, transparent:Boolean) {
  
  def getRawBytes() = {
    val data = this.raster.data
    val cols = this.raster.cols
    val rows = this.raster.rows

    val bb = ByteBuffer.allocate(cols * rows * 3)
    var i = 0
    while (i < rows) {
      // for some reason PNGWriter is reversed... ugh
      val y = rows - i - 1
      var x = 0
      while (x < cols) {
        val z = data(y * cols + x)
        bb.put(((z >> 16) & 0xFF).toByte)
        bb.put(((z >> 8) & 0xFF).toByte)
        bb.put(((z) & 0xFF).toByte)
        x += 1
      }
      i += 1
    }
    bb
  }

  def render() = {
    val bb = this.getRawBytes
    PNGWriter.makeByteArray(bb, this.raster.rows, this.raster.cols, this.bg, this.transparent)
  }

  def write() {
    val bb = this.getRawBytes
    val f = new File(this.path)
    PNGWriter.write(f, bb, this.raster.rows, this.raster.cols, this.bg, this.transparent)
  }
}
