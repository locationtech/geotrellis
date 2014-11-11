package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import geotrellis.vector._

import java.nio.{ByteBuffer, ByteOrder}

import collection.mutable.ArrayBuffer

import spire.syntax.cfor._

case class MalformedShapeIndexFileException(msg: String) extends RuntimeException(msg)

object ShapeIndexFileReader {

  def apply(path: String): ShapeIndexFileReader =
    if (path.endsWith(".shx")) apply(Filesystem.slurp(path))
    else throw new MalformedShapeIndexFileException("Bad file ending (must be .shx).")

  def apply(bytes: Array[Byte]): ShapeIndexFileReader =
    new ShapeIndexFileReader(ByteBuffer.wrap(bytes, 0, bytes.size))

}

class ShapeIndexFileReader(byteBuffer: ByteBuffer) extends ShapeHeaderReader {

  lazy val read: ShapeIndexFile = {
    val boundingBox = readHeader(byteBuffer)

    byteBuffer.order(ByteOrder.BIG_ENDIAN)

    val size = byteBuffer.remaining / 8
    val offsets = Array.ofDim[Int](size)
    val sizes = Array.ofDim[Int](size)

    var idx = 0
    while (byteBuffer.remaining > 0) {
      offsets(idx) = byteBuffer.getInt
      sizes(idx) = byteBuffer.getInt
      idx += 1
    }

    ShapeIndexFile(offsets, sizes)
  }

}
