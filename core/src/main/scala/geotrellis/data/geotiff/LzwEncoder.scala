package geotrellis.data.geotiff

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

import scala.collection.mutable
import scala.annotation.switch

import scala.math.{ceil, min}

import geotrellis._

/**
 * Implements LZW compression encoding.
 *
 * The algorithm is described in detail in the TIFF 6.0 spec, pages 58-61.
 *
 * Each code written out uses 9-12 bits (which explains some the buffering we
 * have to do). Codes 0-255 are reserved for single bytes; code 256 is
 * reserved as CLEAR and code 257 is reserved as END. Thus, all multi-byte
 * codes span from 258 to 4095 (largest possible 12-bit value).
 */
object LzwEncoder {
  def render(encoder:Encoder) = LzwEncoder(encoder).render()

  def apply(encoder:Encoder) = encoder.settings match {
    case Settings(IntSample, Floating, _, _, _) => new LzwFloatEncoder(encoder)
    case Settings(LongSample, Floating, _, _, _) => new LzwDoubleEncoder(encoder)
    case Settings(ByteSample, _, _, _, _) => new LzwByteEncoder(encoder)
    case Settings(ShortSample, _, _, _, _) => new LzwShortEncoder(encoder)
    case Settings(IntSample, _, _, _, _) => new LzwIntEncoder(encoder)
    case s => sys.error("can't encoder %s" format s)
  }
}

class LzwByteEncoder(encoder:Encoder) extends LzwEncoder(encoder) {
  def handleCell(i:Int) {
    var z = data.apply(i)
    if (isNoData(z)) z = encoder.settings.nodataInt
    handleByte(z)
  }
}

class LzwShortEncoder(encoder:Encoder) extends LzwEncoder(encoder) {
  def handleCell(i:Int) {
    var z = data.apply(i)
    if (isNoData(z)) z = encoder.settings.nodataInt
    handleByte(z >> 8)
    handleByte(z & 0xff)
  }
}

class LzwIntEncoder(encoder:Encoder) extends LzwEncoder(encoder) {
  def handleCell(i:Int) {
    var z = data.apply(i)
    if (isNoData(z)) z = encoder.settings.nodataInt
    handleByte(z >> 24)
    handleByte((z >> 16) & 0xff)
    handleByte((z >> 8) & 0xff)
    handleByte(z & 0xff)
  }
}

class LzwFloatEncoder(encoder:Encoder) extends LzwEncoder(encoder) {
  import java.lang.Float.floatToRawIntBits
  private val ndf:Float = if (encoder.settings.esriCompat) Float.MinValue else Float.NaN
  private val ndx:Int = floatToRawIntBits(ndf)

  def handleCell(i:Int) {
    var z = i2d(data(i))
    val n = if (isNoData(z)) ndx else floatToRawIntBits(z.toFloat)
    handleByte(n >> 24)
    handleByte((n >> 16) & 0xff)
    handleByte((n >> 8) & 0xff)
    handleByte(n & 0xff)
  }
}

class LzwDoubleEncoder(encoder:Encoder) extends LzwEncoder(encoder) {
  import java.lang.Double.doubleToRawLongBits
  private val ndf:Double = if (encoder.settings.esriCompat) Double.MinValue else Double.NaN
  private val ndx:Long = doubleToRawLongBits(ndf)

  def handleCell(i:Int) {
    var z = i2d(data(i))
    val n = if (isNoData(z)) ndx else doubleToRawLongBits(z)
    handleByte((n >> 56).toInt)
    handleByte(((n >> 48) & 0xff).toInt)
    handleByte(((n >> 40) & 0xff).toInt)
    handleByte(((n >> 32) & 0xff).toInt)
    handleByte(((n >> 24) & 0xff).toInt)
    handleByte(((n >> 16) & 0xff).toInt)
    handleByte(((n >> 8) & 0xff).toInt)
    handleByte((n & 0xff).toInt)
  }
}

/**
 * Abstract class to maintain internal LZW encoder state.
 *
 * This class should be overridden for different bit widths and formats (e.g.
 * 1-bit, 8-bit unsigned, 32-bit floating-point, etc).
 *
 * Since the LZW algorithm is very stateful this class uses its own mutable
 * state to keep track of things like the string table. It is not intended to
 * be used directly; you should use LzwEncoder.render(...) instead.
 */
abstract class LzwEncoder(encoder:Encoder) {
  // useful stuff to pull out of an encoder
  val cols = encoder.cols
  val rows = encoder.rows
  val data = encoder.data
  val imageStartOffset = encoder.imageStartOffset
  val rowsPerStrip = encoder.rowsPerStrip

  // data output stream to write bytes into
  val dmg = new DataOutputStream(encoder.img)

  // the table mapping strings to LZW codes
  var stringTable:mutable.Map[String, Int] = null

  // the current input byte, as named in the TIFF 6.0 LZW section
  var omega:String = ""

  // the number of bits we're currently using for LZW codes (ranges 9-12)
  var codeBits:Int = 0

  // the next code we want to use (ranges 258-4094)
  var nextCode:Int = 0

  // the next point we need to increase codeBits (ranges 512-4096)
  var nextLimit:Int = 0

  // buffer and bufpos are used to buffer the output of 9-12 bit sequences
  var buffer:Int = 0
  var bufpos:Int = 0

  // offsets/lengths track the size/boundaries of each compressed strip
  val offsets = Array.ofDim[Int](encoder.numStrips)
  val lengths = Array.ofDim[Int](encoder.numStrips)

  // offset stores the current number of bytes we've output
  var offset = 0

  def xyz(o:String) = o.map(_.toInt)

  /**
   * Reinitializes all state related to the string table. After this happens
   * codes will reset to 258 and being 9-bit.
   *
   * It's important to remember to flush the omega variable before calling
   * this method, and also to call it while there is still room in the 12-bit
   * string table (i.e. once 4094 has been added).
   */
  def initStringTable() {
    stringTable = mutable.Map.empty[String, Int]
    for (i <- 0 until 256) stringTable(i.toChar.toString) = i
    nextCode = 258
    nextLimit = 512
    codeBits = 9
  }

  /**
   * Test if the given string is available in the table.
   */
  def hasCode(s:String): Boolean = stringTable.contains(s)

  /**
   * Retrieve the given string from the string table.
   *
   * This method will crash if the string can't be found.
   */
  def getCode(s:String): Int = {
    if (!stringTable.contains(s)) {
      // stringTable.toList.sorted.foreach {
      //   case (k, v) => println("  k=%s v=%s" format (xyz(k), v))
      // }
      sys.error(s"no code found for ${xyz(s)}")
    }
    stringTable(s)
  }

  /**
   * Add the given string to the string table.
   *
   * If the string table fills up, this method will write out omega (as a
   * 12-bit code) and then reset the string table and omega.
   */
  def addCode(str:String) {
    val code = nextCode
    stringTable(str) = code
    nextCode += 1

    if (nextCode == 4095) {
      writeCode(256)
      initStringTable()
    } else if (nextCode == nextLimit) {
      codeBits += 1
      nextLimit <<= 1
    }
  }

  def setOmega(s:String) { omega = s }

  /**
   * Process a single cell of raster data according to LZW.
   *
   * This should encode the cell's value appropriately into bytes (taking into
   * account NODATA, sign, etc) and then make one or more handleByte calls.
   */
  def handleCell(i:Int):Unit

  /**
   * Process a single byte of image data according to LZW.
   *
   * This is the heart of the LZW algorithm.
   */
  def handleByte(k:Int) {
    val c = k.toChar
    val s = omega + c
    if (hasCode(s)) {
      setOmega(s)
    } else {
      writeCode(getCode(omega))
      setOmega(c.toString)
      addCode(s)
    }
  }

  def flushByte(b:Byte) { dmg.writeByte(b) }

  /**
   * Write a 9-12 bit code to our output.
   *
   * Due to byte-alignment issues we use a buffering strategy, writing out
   * complete 2 byte "chunks" at a time.
   */
  def writeCode(code:Int) {
    //println("**writeCode(%s)" format code)
    buffer |= code << (32 - codeBits - bufpos)
    bufpos += codeBits
    if (bufpos > 16) {
      flushByte(((buffer >> 24) & 0xff).toByte)
      flushByte(((buffer >> 16) & 0xff).toByte)
      buffer <<= 16
      bufpos -= 16
      offset += 2
    }
  }

  /**
   * Write out all remaining buffered data. This will have the effect of
   * possibly padding the last value up to a byte boundary.
   */
  def flushBuffer() {
    while (bufpos > 0) {
      val b = ((buffer >> 24) & 0xff).toByte
      flushByte(b)
      buffer <<= 8
      bufpos -= 8
      offset += 1
    }
    bufpos = 0
    buffer = 0
  }


  def render():(Array[Int], Array[Int]) = {
    var row = 0
    var strip = 0
    while (row < rows) {

      // if we are starting a strip, initialize the state we need
      if (row % rowsPerStrip == 0) {
        offsets(strip) = imageStartOffset + offset
        initStringTable()
        writeCode(256)
      }

      var col = 0
      val rowspan = row * cols
      while (col < cols) {
        handleCell(rowspan + col)
        col += 1
      }

      row += 1

      // if we are ending a strip, finish writing the data and store the length
      if (row % rowsPerStrip == 0 || row == rows) {
        writeCode(getCode(omega))
        setOmega("")
        writeCode(257)
        flushBuffer()
        lengths(strip) = imageStartOffset + offset - offsets(strip)
        strip += 1
      }
    }

    // return the positions and lenghts of each strip
    (offsets, lengths)
  }
}
