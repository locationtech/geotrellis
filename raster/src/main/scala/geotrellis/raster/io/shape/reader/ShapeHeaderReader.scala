package geotrellis.raster.io.shape.reader

import java.nio.{ByteBuffer, ByteOrder}

import spire.syntax.cfor._

case class MalformedShapeHeaderException(msg: String) extends RuntimeException(msg)

trait ShapeHeaderReader {

  import ShapeType._

  protected def readHeader(byteBuffer: ByteBuffer): Array[Double] = {
    val boundingBox = Array.ofDim[Double](8)
    val oldBBOrder = byteBuffer.order

    byteBuffer.order(ByteOrder.BIG_ENDIAN)

    val fileCode = byteBuffer.getInt
    if (fileCode != 9994)
      throw new MalformedShapeFileException(s"Wrong file code, $fileCode != 9994.")

    for (i <- 0 until 5)
      if (byteBuffer.getInt != 0)
        throw new MalformedShapeFileException("Malformed file header.")

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

    val fileSize = byteBuffer.getInt
    if (fileSize != byteBuffer.limit)
      throw new MalformedShapeFileException(
        s"Malformed file size, $fileSize != ${byteBuffer.limit}.")

    val version = byteBuffer.getInt
    if (version != 1000)
      throw new MalformedShapeFileException(s"Wrong version, $fileCode != 1000.")

    val shapeType = byteBuffer.getInt
    if (!ValidValues.contains(shapeType))
      throw new MalformedShapeFileException(s"Malformed shape type $shapeType.")

    cfor(0)(_ < boundingBox.size, _ + 1) { i =>
      boundingBox(i) = byteBuffer.getDouble
    }

    byteBuffer.order(oldBBOrder)

    boundingBox
  }

}
