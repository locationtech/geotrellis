package geotrellis.vector.io.shape.reader

import geotrellis.vector._
import geotrellis.vector.io.FileSystem

import java.nio.{ByteBuffer, ByteOrder}

import collection.mutable.ArrayBuffer

import spire.syntax.cfor._

case class MalformedShapePointFileException(msg: String) extends RuntimeException(msg)

object ShapePointFileReader {
  final val FileExtension = ".shp"

  def apply(path: String): ShapePointFile =
    if (path.endsWith(FileExtension)) {
      val bytes = 
        FileSystem.slurp(path)
      apply(bytes)
    } else {
      throw new MalformedShapePointFileException(s"Bad file ending (must be $FileExtension).")
    }

  def apply(bytes: Array[Byte]): ShapePointFile =
    apply(ByteBuffer.wrap(bytes, 0, bytes.size))

  /** ShapePointFileReader is used to read the actual geometries form the shapefile.
    * It reads points out of sections of the byte buffer based on what type of geometry is being read.
    * 
    * @param      byteBuffer           ByteBuffer containing the data to read.
    */
  def apply(byteBuffer: ByteBuffer): ShapePointFile = {
    val extent = ShapeHeaderReader(byteBuffer)

    val geomBuffer = ArrayBuffer[Geometry]()

    while (byteBuffer.remaining > 0) {
      byteBuffer.order(ByteOrder.BIG_ENDIAN)
      val nr = byteBuffer.getInt
      val size = byteBuffer.getInt

      byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      geomBuffer += GeometryReader(byteBuffer.getInt)(byteBuffer)
    }

    ShapePointFile(geomBuffer.toArray, extent)
  }
}
