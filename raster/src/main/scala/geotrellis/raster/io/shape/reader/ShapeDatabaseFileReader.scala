package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import java.nio.{ByteBuffer, ByteOrder}

import java.util.{Calendar, Date}

import spire.syntax.cfor._

case class MalformedShapeDatabaseFileException(msg: String) extends RuntimeException(msg)

/**
  * Reads a DBase III file.
  */
object ShapeDatabaseFileReader {

  def apply(path: String): ShapeDatabaseFileReader =
    if (path.endsWith(".dbf")) apply(Filesystem.slurp(path))
    else throw new MalformedShapeFileException("Bad file ending (must be .dbf).")

  def apply(bytes: Array[Byte]): ShapeDatabaseFileReader =
    new ShapeDatabaseFileReader(ByteBuffer.wrap(bytes, 0, bytes.size))

}

class ShapeDatabaseFileReader(byteBuffer: ByteBuffer) {

  class DBaseField(
    val fieldName: String,
    val fieldType: Char,
    val offset: Int,
    val length: Int,
    val decimalCount: Int)

  private val DBaseVersion = 3

  private val FileDescriptorSize = 32

  lazy val read: ShapeDatabaseFile = {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

    if (byteBuffer.get != DBaseVersion)
      throw new MalformedShapeDatabaseFileException(
        "bad DBase version, must be DBase III.")

    val year = byteBuffer.get
    val month = byteBuffer.get
    val day = byteBuffer.get

    Calendar c = Calendar.getInstance
    c.set(Calendar.YEAR, if (year > 90) year + 1900 else year + 2000)
    c.set(Calendar.MONTH, month)
    c.set(Calendar.DATE, day)

    val date = c.getTime

    val recordCount = byteBuffer.getInt

    val headerLength = byteBuffer.get | (byteBuffer.get << 8)

    val recordLength = byteBuffer.get | (byteBuffer.get << 8)

    byteBuffer.position(byteBuffer.position + 20)

    val records = (headerLength - FileDescriptorSize - 1) / FileDescriptorSize
    val fieldBuffer = ListBuffer[DBaseField]()

    cfor(0)(_ < records, _ + 1) { i =>

    }
  }

}
