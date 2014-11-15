package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import java.nio.{ByteBuffer, ByteOrder}

import java.util.{Calendar, Date}

import spire.syntax.cfor._

case class MalformedShapeDBaseFileException(msg: String) extends RuntimeException(msg)

/**
  * Reads a DBase III file (.dbf) for shape files.
  *
  * TODO: Charset crap?
  */
object ShapeDBaseFileReader {

  def apply(path: String): ShapeDBaseFileReader =
    if (path.endsWith(".dbf")) apply(Filesystem.slurp(path))
    else throw new MalformedShapeDBaseFileException("Bad file ending (must be .dbf).")

  def apply(bytes: Array[Byte]): ShapeDBaseFileReader =
    new ShapeDBaseFileReader(ByteBuffer.wrap(bytes, 0, bytes.size))

}

class ShapeDBaseFileReader(byteBuffer: ByteBuffer) {

  case class DBaseField(
    fieldName: String,
    fieldType: Char,
    offset: Int,
    length: Int,
    decimalCount: Int)

  private val DBaseVersion = 3

  private val FileDescriptorSize = 32

  private val MillisPerDay = 1000 * 60 * 60 * 24

  private val MillisSince4713 = -210866803200000L

  lazy val read: ShapeDBaseFile = {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

    if (byteBuffer.get != DBaseVersion)
      throw new MalformedShapeDBaseFileException("Bad DBase version, must be DBase III.")

    val year = byteBuffer.get
    val month = byteBuffer.get
    val day = byteBuffer.get

    val c = Calendar.getInstance
    c.set(Calendar.YEAR, year + (if (year > 90) 1900 else 2000))
    c.set(Calendar.MONTH, month)
    c.set(Calendar.DATE, day)

    val date = c.getTime

    val recordCount = byteBuffer.getInt

    val headerLength = (byteBuffer.get & 0xff) | ((byteBuffer.get & 0xff) << 8)

    val recordLength = (byteBuffer.get & 0xff)| ((byteBuffer.get & 0xff) << 8)

    byteBuffer.position(byteBuffer.position + 20)

    val records = (headerLength - FileDescriptorSize - 1) / FileDescriptorSize

    println(records)

    val fields = Array.ofDim[Option[DBaseField]](records)

    val nameBuffer = Array.ofDim[Byte](11)
    var largestField = 0
    cfor(0)(_ < records, _ + 1) { i =>
      byteBuffer.get(nameBuffer)
      var nameBufferString = new String(nameBuffer)
      val nullPoint = nameBufferString.indexOf(0)

      val name =
        if (nullPoint != -1) nameBufferString.substring(0, nullPoint)
        else nameBufferString

      val typC = byteBuffer.get
      val typ = (if (typC < 0) typC + 256 else typC).toChar

      val offset = byteBuffer.getInt

      val lengthByte = byteBuffer.get.toInt
      println(lengthByte)
      val length =
        if (lengthByte < 0) lengthByte + 256
        else lengthByte

      largestField = math.max(largestField, length)

      val decimalCount = byteBuffer.get.toInt

      fields(i) =
        if (length > 0) Some(DBaseField(name, typ, offset, length, decimalCount))
        else None

      byteBuffer.position(byteBuffer.position + 14)
    }

    ShapeDBaseFile(fields.map(readDBaseRecord(_)).toArray)
  }

  private def readDBaseRecord(fieldOption: Option[DBaseField]) = fieldOption match {
    case Some(field) => {
      byteBuffer.position(field.offset)

      field.fieldType match {
        case 'l' | 'L' => readLogicalRecord(field)
        case 'c' | 'C' => readStringRecord(field)
        case 'd' | 'D' => readDateRecord(field)
        case '@' => readTimestampRecord(field)
        case 'n' | 'N' => readLongRecord(field) match {
          case None => {
            byteBuffer.position(field.offset)
            readDoubleRecord(field)
          }
          case x => x
        }
        case 'f' | 'F' => readDoubleRecord(field)
        case invalidFieldType => throw new MalformedShapeDBaseFileException(
          s"Invalid field type $invalidFieldType."
        )
      }
    }
    case None => None
  }

  private def readBytes(length: Int) = {
    val bytes = Array.ofDim[Byte](length)
    byteBuffer.get(bytes)

    bytes
  }

  private def readLogicalRecord(field: DBaseField) = byteBuffer.get.toChar match {
    case 't' | 'T' | 'y' | 'Y' => Some(LogicalDBaseRecord(true))
    case 'f' | 'F' | 'n' | 'N' => Some(LogicalDBaseRecord(false))
    case _ => None
  }

  private def readStringRecord(field: DBaseField) = {
    val bytes = readBytes(field.length)

    if (bytes(0) == '\0') None
    else Some(StringDBaseRecord(new String(bytes, "ASCII")))
  }

  private def readDateRecord(field: DBaseField) = {
    val bytes = readBytes(8)
    if (bytes.filter(_ != '0').isEmpty) None
    else {
      val year = new String(bytes.take(4)).toInt
      val month = new String(bytes.drop(4).take(2)).toInt
      val day = new String(bytes.drop(6).take(2)).toInt

      val c = Calendar.getInstance
      c.set(Calendar.YEAR, year)
      c.set(Calendar.MONTH, month)
      c.set(Calendar.DATE, day)

      Some(DateDBaseRecord(c.getTime))
    }
  }

  private def readTimestampRecord(field: DBaseField) = {
    val bo = byteBuffer.order
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val time = byteBuffer.getInt
    val days = byteBuffer.getInt

    val c = Calendar.getInstance
    c.setTimeInMillis(days * MillisPerDay + MillisSince4713 + time)

    byteBuffer.order(bo)
    Some(DateDBaseRecord(c.getTime))
  }

  private def readLongRecord(field: DBaseField) = try {
    Some(LongDBaseRecord(new String(readBytes(field.length)).toLong))
  } catch {
    case e: NumberFormatException => None
  }

  private def readDoubleRecord(field: DBaseField) = try {
    Some(DoubleDBaseRecord(new String(readBytes(field.length)).toDouble))
  } catch {
    case e: NumberFormatException => None
  }

}
