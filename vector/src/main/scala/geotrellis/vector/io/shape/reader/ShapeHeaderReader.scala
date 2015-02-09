package geotrellis.vector.io.shape.reader

import geotrellis.vector._

import java.nio.{ByteBuffer, ByteOrder}

import spire.syntax.cfor._

case class MalformedShapeFileHeaderException(msg: String) extends RuntimeException(msg)

object ShapeHeaderReader {

  def apply(byteBuffer: ByteBuffer): Extent = {
    val boundingBox = Array.ofDim[Double](8)
    val oldBBOrder = byteBuffer.order

    byteBuffer.order(ByteOrder.BIG_ENDIAN)

    val fileCode = byteBuffer.getInt
    if (fileCode != 9994)
      throw new MalformedShapeFileHeaderException(s"Wrong file code, $fileCode != 9994.")

    cfor(0)(_ < 5, _ + 1) { i =>
      if (byteBuffer.getInt != 0)
        throw new MalformedShapeFileHeaderException("Malformed file header.")
    }

    val fileSize = byteBuffer.getInt // This is in 16 bit words.
    if (fileSize * 2 != byteBuffer.limit)
      throw new MalformedShapeFileHeaderException(
        s"Malformed file size, $fileSize != ${byteBuffer.limit}.")

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

    val version = byteBuffer.getInt
    if (version != 1000)
      throw new MalformedShapeFileHeaderException(s"Wrong version, $fileCode != 1000.")

    // Skip the Shape code. Not sure why there is one at the head of the file, each entry has one.
    byteBuffer.getInt

    cfor(0)(_ < boundingBox.size, _ + 1) { i =>
      boundingBox(i) = byteBuffer.getDouble
    }

    byteBuffer.order(oldBBOrder)
    Extent(boundingBox(0), boundingBox(1), boundingBox(2), boundingBox(3))
  }

}
