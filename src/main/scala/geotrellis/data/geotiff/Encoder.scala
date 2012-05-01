package geotrellis.data.geotiff

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

import scala.math.abs

import geotrellis._

// TODO: support writing more than one strip
// TODO: compression?
// TODO: color?

/**
 * tag types
 *  1: 8-bit unsigned int
 *  2: 7-bit ascii code (must end in NUL)
 *  3: 16-bit unsigned int
 *  4: 32-bit unsigned int
 *  5: rational: 32-bit unsigned numerator and denominator
 *  6: 8-bit signed int
 *  7: undefined 8-bit
 *  8: 16-bit signed int
 *  9: 32-bit signed int
 * 10: signed rational (like 5 but signed)
 * 11: 32-bit floating point
 * 12: 64-bit floating point
 */
object Const {
  final def uint8 = 1      // 8-bit unsigned int
  final def ascii = 2      // 7-bit ascii code (must end in NUL)
  final def uint16 = 3     // 16-bit unsigned int
  final def uint32 = 4     // 32-bit unsigned int
  final def rational = 5   // rational: 2 uint32 values (numerator/denominator)
  final def sint8 = 6      // 8-bit signed int
  final def undef8 = 7     // undefined 8-bit
  final def sint16 = 8     // 16-bit signed int
  final def sint32 = 9     // 32-bit signed int
  final def srational = 10 // signed rational: 2 sint32 values (n/d)
  final def float32 = 11   // 32-bit floating point
  final def float64 = 12   // 64-bit floating point

  final def unsigned = 1
  final def signed = 2
  final def floating = 3
}

sealed abstract class SampleSize(val bits:Int)
case object BitSample extends SampleSize(1)
case object ByteSample extends SampleSize(8)
case object ShortSample extends SampleSize(16)
case object IntSample extends SampleSize(32)

sealed abstract class SampleFormat(val kind:Int)
case object Unsigned extends SampleFormat(Const.unsigned)
case object Signed extends SampleFormat(Const.signed)
case object Floating extends SampleFormat(Const.floating)

case class Settings(size:SampleSize, format:SampleFormat)

class Encoder(dos:DataOutputStream, raster:IntRaster, settings:Settings) {
  val data = raster.data
  val re = raster.rasterExtent
  val cols = re.cols
  val rows = re.rows
  val e = re.extent

  //final def bitsPerSample = 1
  //final def bitsPerSample = 8
  //final def bitsPerSample = 16
  //final def bitsPerSample = 32
  final def bitsPerSample = settings.size.bits

  //final def sampleFormat = Const.unsigned
  //final def sampleFormat = Const.signed
  final def sampleFormat = settings.format.kind

  // no data value used in the actual raster
  final def noDataValue = {
    if (sampleFormat == Const.signed) (1L << (bitsPerSample - 1)).toInt
    else if (sampleFormat == Const.unsigned) ((1L << bitsPerSample) - 1).toInt
    else if (sampleFormat == Const.floating) sys.error("float not supported")
    else sys.error("unknown sample format: %s" format sampleFormat)
  }

  // no data value represented as a string
  final def noDataString = {
    if (sampleFormat == Const.signed) "-" + (1L << (bitsPerSample - 1)).toString
    else if (sampleFormat == Const.unsigned) ((1L << bitsPerSample) - 1).toString
    else if (sampleFormat == Const.floating) sys.error("float not supported")
    else sys.error("unknown sample format: %s" format sampleFormat)
  }

  // the number of TIFF headers we want to write
  final def numEntries = 18

  // we often need to "defer" writing data blocks, but keep track of how much
  // data would be written, to correctly compute future data block addresses.
  // we use 'todo' (backed by 'baos') to store this kind of data, and the
  // 'dataOffset' function to compute these addresses.
  val baos = new ByteArrayOutputStream()
  val todo = new DataOutputStream(baos)

  // the address we will start writing the image to
  final def imageStartOffset = 8

  // the addres we expect to see IFD information at
  final def ifdOffset = imageStartOffset + img.size

  // the current address we will write (non-image) "data blocks" to
  final def dataOffset = ifdOffset + 2 + (numEntries * 12) + 4 + baos.size

  // the byte array we will write all out 'strips' of image data to. we need
  // this to correctly compute addresses beyond the image (i.e. TIFF headers
  // and data blocks).
  val img = new ByteArrayOutputStream()

  // the number of bytes we've written to our 'dos'. only used for debugging.
  var index:Int = 0

  // used to immediately write values to our ultimate destination.
  def writeByte(value:Int) { dos.writeByte(value); index += 1 }
  def writeShort(value:Int) { dos.writeShort(value); index += 2 }
  def writeInt(value:Int) { dos.writeInt(value); index += 4 }
  def writeLong(value:Long) { dos.writeLong(value); index += 8 }
  def writeFloat(value:Float) { dos.writeFloat(value); index += 4 }
  def writeDouble(value:Double) { dos.writeDouble(value); index += 8 }

  // used to write data blocks to our "deferred" byte array.
  def todoByte(value:Int) { todo.writeByte(value) }
  def todoShort(value:Int) { todo.writeShort(value) }
  def todoInt(value:Int) { todo.writeInt(value) }
  def todoLong(value:Long) { todo.writeLong(value) }
  def todoFloat(value:Float) { todo.writeFloat(value) }
  def todoDouble(value:Double) { todo.writeDouble(value) }

  // high-level function to defer writing geotiff tags
  def todoGeoTag(tag:Int, loc:Int, count:Int, offset:Int) {
    todoShort(tag)
    todoShort(loc)
    todoShort(count)
    todoShort(offset)
  }

  // high-level function to write strings (char arrays). this will do immediate
  // writes for the header information, and possibly also do deferred writes to
  // the data blocks (if the null-terminated string data can't fit in 4 bytes).
  def writeString(tag:Int, s:String) {
    writeShort(tag)
    writeShort(2)
    writeInt(s.length + 1)
    if (s.length < 4) {
      var j = 0
      while (j < s.length) { writeByte(s(j).toInt); j += 1 }
      while (j < 4) { writeByte(0); j += 1 }
    } else {
      writeInt(dataOffset)
      var j = 0
      while (j < s.length) { todoByte(s(j).toInt); j += 1 }
      todoByte(0)
    }
  }

  // high-level function to immediately write a TIFF tag. if value is an offset
  // then the user is responsible for making sure it is valid.
  def writeTag(tag:Int, typ:Int, count:Int, value:Int) {
    writeShort(tag)
    writeShort(typ)
    writeInt(count)
    if (count > 1) {
      writeInt(value)
    } else if (typ == 1 || typ == 6) {
      writeByte(value)
      writeByte(0)
      writeShort(0)
    } else if (typ == 3 || typ == 8) {
      writeShort(value)
      writeShort(0)
    } else {
      writeInt(value)
    }
  }

  // immediately write a byte array to the output
  def writeByteArrayOutputStream(b:ByteArrayOutputStream) {
    b.writeTo(dos)
    index += b.size
  }

  def renderImage():Array[Int] = {
    val dmg = new DataOutputStream(img)
    var row = 0

    val bits = bitsPerSample
    val nd = noDataValue

    while (row < rows) {
      val rowspan = row * cols
      var col = 0
      while (col < cols) {
        var z = data(rowspan + col)
        if (z == Int.MinValue) z = nd
        if (bits == 8) dmg.writeByte(z)
        else if (bits == 16) dmg.writeShort(z)
        else if (bits == 32) dmg.writeInt(z)
        else sys.error("unsupported bits/sample: %s" format bits)
        col += 1
      }
      row += 1
    }
    Array(imageStartOffset)
  }

  def write() {
    // 0x0000: first 4 bytes of signature
    writeInt(0x4d4d002a)

    // render image (does not write output)
    val stripOffsets = renderImage()

    // 0x0004: offset to the first IFD
    writeInt(ifdOffset)

    // 0x0008: image data
    writeByteArrayOutputStream(img)

    // number of directory entries
    writeShort(numEntries)

    // 1. image width (cols)
    writeTag(0x0100, Const.uint32, 1, cols)

    // 2. image length (rows)
    writeTag(0x0101, Const.uint32, 1, rows)

    // 3. bits per sample
    writeTag(0x0102, Const.uint16, 1, bitsPerSample)

    // 4. compression is off (1)
    writeTag(0x0103, Const.uint16, 1, 1)

    // 5. photometric interpretation, black is zero (1)
    writeTag(0x0106, Const.uint16, 1, 1)

    // 6. strip offsets (actual image data)
    if (stripOffsets.length == 1) {
      // if we have one strip, write it's address in the tag
      writeTag(0x0111, Const.uint32, 1, stripOffsets(0))
    } else {
      // if we have multiple strips, write each addr to a data block
      writeTag(0x0111, Const.uint32, stripOffsets.length, dataOffset)
      stripOffsets.foreach(addr => todoInt(addr))
    }

    // 7. 1 sample per pixel
    writeTag(0x0115, Const.uint16, 1, 1)

    // 8. 'rows' rows per strip
    writeTag(0x0116, Const.uint32, 1, rows)

    // 9. strip byte counts (1 strip)
    writeTag(0x0117, Const.uint16, 1, img.size)

    // 10. y resolution, 1 pixel per unit
    writeTag(0x011a, Const.rational, 1, dataOffset)
    todoInt(1)
    todoInt(1)

    // 11. x resolution, 1 pixel per unit
    writeTag(0x011b, Const.rational, 1, dataOffset)
    todoInt(1)
    todoInt(1)

    // 12. single image plane (1)
    writeTag(0x011c, Const.uint16, 1, 1)

    // 13. resolution unit is undefined (1)
    writeTag(0x0128, Const.uint16, 1, 1)

    // 14. sample format (1=unsigned, 2=signed, 3=floating point)
    writeTag(0x0153, Const.uint16, 1, sampleFormat)

    // 15. model pixel scale tag (points to doubles sX, sY, sZ with sZ = 0)
    writeTag(0x830e, Const.float64, 3, dataOffset)
    todoDouble(re.cellwidth)  // sx
    todoDouble(re.cellheight) // sy
    todoDouble(0.0)           // sz = 0.0

    // 16. model tie point (doubles I,J,K (grid) and X,Y,Z (geog) with K = Z = 0.0)
    writeTag(0x8482, Const.float64, 6, dataOffset)
    todoDouble(0.0)    // i
    todoDouble(0.0)    // j
    todoDouble(0.0)    // k = 0.0
    todoDouble(e.xmin) // x
    todoDouble(e.ymax) // y
    todoDouble(0.0)    // z = 0.0

    // 17. geo key directory tag (4 geotags each with 4 short values)
    writeTag(0x87af, Const.uint16, 4 * 4, dataOffset)
    todoGeoTag(1, 1, 2, 3)       // geotif 1.2, 3 more tags
    todoGeoTag(1024, 0, 1, 1)    // projected data (undef=0, projected=1, geographic=2, geocenteric=3)
    todoGeoTag(1025, 0, 1, 1)    // area data (undef=0, area=1, point=2)
    todoGeoTag(3072, 0, 1, 3857) // gt projected cs type (3857)

    // 18. nodata as string
    writeString(0xa481, noDataString)

    // no more ifd entries
    writeInt(0)

    // write all our data blocks
    writeByteArrayOutputStream(baos)

    dos.flush()
  }
}

object Encoder {
  def writeBytes(raster:IntRaster, settings:Settings) = {
    val baos = new ByteArrayOutputStream()
    val dos = new DataOutputStream(baos)
    val encoder = new Encoder(dos, raster, settings)
    encoder.write()
    baos.toByteArray
  }

  def writePath(path:String, raster:IntRaster, settings:Settings) {
    val fos = new FileOutputStream(new File(path))
    val dos = new DataOutputStream(fos)
    val encoder = new Encoder(dos, raster, settings)
    encoder.write()
    fos.close()
  }
}
