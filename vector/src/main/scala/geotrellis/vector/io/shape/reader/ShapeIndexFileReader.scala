package geotrellis.vector.io.shape.reader

import geotrellis.vector._
import geotrellis.vector.io.FileSystem

import java.nio.{ByteBuffer, ByteOrder}

import collection.mutable.ArrayBuffer

import spire.syntax.cfor._

case class MalformedShapeIndexFileException(msg: String) extends RuntimeException(msg)

object ShapeIndexFileReader {

  val FileExtension = ".shx"

  def apply(path: String): ShapeIndexFileReader =
    if (path.endsWith(FileExtension)) apply(FileSystem.slurp(path))
    else throw new MalformedShapeIndexFileException(
      s"Bad file ending (must be .$FileExtension)."
    )

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
