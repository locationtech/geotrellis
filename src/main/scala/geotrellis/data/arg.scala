package geotrellis.data

import scala.xml._
import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

import geotrellis._
import geotrellis.util._

import geotrellis.IntRaster
import geotrellis.process._

final class ArgReadState(data:Either[String, Array[Byte]],
                         val layer:RasterLayer,
                         val target:RasterExtent) extends ReadState {
  private var src:ByteBuffer = null

  def getNoDataValue = 0

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos, size)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos, size)
    }
  }

  @inline
  def assignFromSource(sourceIndex:Int, dest:Array[Int], destIndex:Int) {
    dest(destIndex) = src.get(sourceIndex)
  }
}

//object ArgReadState {
//  def apply(path:String, layer:RasterLayer, target:RasterExtent) = new ArgReadState(Left(path), layer, target)
//
//  def fromPath(path:String, layer:RasterLayer, target:RasterExtent) = new ArgReadState(Left(path), layer, target)
//  def fromCache(bytes:Array[Byte], layer:RasterLayer, target:RasterExtent) = new ArgReadState(Right(bytes), layer, target)
//}

object ArgReader extends FileReader {
  def readStateFromCache(b:Array[Byte], rl:RasterLayer, re:RasterExtent) = new ArgReadState(Right(b), rl, re)
  def readStateFromPath(p:String, rl:RasterLayer, re:RasterExtent) = new ArgReadState(Left(p), rl, re)
}


object ArgWriter extends Writer {
  def rasterType = "arg"
  def dataType = "legacy"

  def write(path:String, raster:IntRaster, name:String) {
    val i = path.lastIndexOf(".")
    val base = path.substring(0, i)
    writeMetadataJSON(base + ".json", name, raster.rasterExtent)
    writeData(base + ".arg", raster)
  }

  private def writeData(path:String, raster:IntRaster) {
    val cols = raster.rasterExtent.cols
    val rows = raster.rasterExtent.rows
    val buffer = Array.ofDim[Byte](cols)

    val bos = new BufferedOutputStream(new FileOutputStream(path))

    var row = 0
    while (row < rows) {
      var col = 0
      while (col < cols) {
        var z = raster.get(col, row)
        buffer(col) = ((z & 0x000000ff)).toByte
        col += 1
      }
      bos.write(buffer)
      row += 1
    }

    bos.close()
  }
}
