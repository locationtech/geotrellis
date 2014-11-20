package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import collection.immutable.HashMap

import java.nio.{ByteBuffer, ByteOrder}

import java.util.{Calendar, Date}

import spire.syntax.cfor._

case class MalformedShapeDBaseFileException(msg: String) extends RuntimeException(msg)

import Charset._

/**
  * Reads a DBase III file (.dbf) for shape files.
  */
object ShapeDBaseFileReader {

  def apply(path: String, charset: Charset = Charset.Ascii): ShapeDBaseFileReader =
    if (path.endsWith(".dbf")) apply(Filesystem.slurp(path), charset)
    else throw new MalformedShapeDBaseFileException("Bad file ending (must be .dbf).")

  def apply(bytes: Array[Byte], charset: Charset): ShapeDBaseFileReader =
    new ShapeDBaseFileReader(ByteBuffer.wrap(bytes, 0, bytes.size), charset)

}

class ShapeDBaseFileReader(byteBuffer: ByteBuffer, charset: Charset = Charset.Ascii) {

  case class DBaseField(
    name: String,
    typ: Char,
    offset: Int,
    length: Int,
    decimalCount: Int)

  private val DBaseVersion = 3

  private val FileDescriptorSize = 32

  private val MillisPerDay = 1000 * 60 * 60 * 24

  private val MillisSince4713 = -210866803200000L

  private def readString(buffer: Array[Byte]) = new String(buffer, charset.code)

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

    val recordLength = (byteBuffer.get & 0xff) | ((byteBuffer.get & 0xff) << 8)

    byteBuffer.position(byteBuffer.position + 20)

    val columns = (headerLength - FileDescriptorSize - 1) / FileDescriptorSize

    val fields = Array.ofDim[Option[DBaseField]](columns)

    val nameBuffer = Array.ofDim[Byte](11)
    cfor(0)(_ < columns, _ + 1) { i =>
      byteBuffer.get(nameBuffer)
      var nameBufferString = readString(nameBuffer)
      val nullPoint = nameBufferString.indexOf(0)

      val name =
        if (nullPoint != -1) nameBufferString.substring(0, nullPoint)
        else nameBufferString

      val typC = byteBuffer.get
      val typ = (if (typC < 0) typC + 256 else typC).toChar

      val offset = byteBuffer.getInt

      val lengthByte = byteBuffer.get.toInt
      val length =
        if (lengthByte < 0) lengthByte + 256
        else lengthByte

      val decimalCount = byteBuffer.get.toInt

      fields(i) =
        if (length > 0) Some(DBaseField(name, typ, offset, length, decimalCount))
        else None

      byteBuffer.position(byteBuffer.position + 14)
    }

    val rows = Array.ofDim[Map[String, ShapeDBaseRecord]](recordCount)
    var fieldOffsets = Array.ofDim[Int](columns)

    cfor(1)(_ < columns, _ + 1) { i =>
      val length = fields(i - 1) match {
        case Some(f) => f.length
        case None => 0
      }
      fieldOffsets(i) = fieldOffsets(i - 1) + length
    }

    cfor(0)(_ < rows.size, _ + 1) { i =>
      var map = HashMap[String, ShapeDBaseRecord]()
      cfor(0)(_ < columns, _ + 1) { j =>
        val offset = headerLength + i * recordLength + fieldOffsets(j) + 1
        map = readDBaseRecord(fields(j), offset) match {
          case Some(r) => map + r
          case None => map
        }
      }

      rows(i) = map
    }

    ShapeDBaseFile(rows)
  }

  private def readDBaseRecord(fieldOption: Option[DBaseField], offset: Int) = fieldOption match {
    case Some(field) => {
      byteBuffer.position(offset)

      val fieldValue: Option[ShapeDBaseRecord] = field.typ match {
        case 'l' | 'L' => readLogicalRecord(field)
        case 'c' | 'C' => readStringRecord(field)
        case 'd' | 'D' => readDateRecord(field)
        case '@' => readTimestampRecord(field)
        case 'n' | 'N' => readLongRecord(field) match {
          case None => {
            byteBuffer.position(offset)
            readDoubleRecord(field)
          }
          case x => x
        }
        case 'f' | 'F' => readDoubleRecord(field)
        case invalidFieldType => throw new MalformedShapeDBaseFileException(
          s"Invalid field type $invalidFieldType."
        )
      }

      fieldValue match {
        case Some(f) => Some(field.name -> f)
        case None => None
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
    val bytes = readBytes(field.length).takeWhile(_ != '\0')

    if (bytes.isEmpty) None
    else Some(StringDBaseRecord(readString(bytes).trim))
  }

  private def readDateRecord(field: DBaseField) = {
    val bytes = readBytes(8)
    if (bytes.filter(_ != '0').isEmpty) None
    else {
      val year = readString(bytes.take(4)).toInt
      val month = readString(bytes.drop(4).take(2)).toInt
      val day = readString(bytes.drop(6).take(2)).toInt

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
    val s = readString(readBytes(field.length)).trim
    Some(LongDBaseRecord(s.toLong))
  } catch {
    case e: NumberFormatException => None
  }

  private def readDoubleRecord(field: DBaseField) = try {
    val s = readString(readBytes(field.length)).trim
    Some(DoubleDBaseRecord(s.toDouble))
  } catch {
    case e: NumberFormatException => None
  }

}
