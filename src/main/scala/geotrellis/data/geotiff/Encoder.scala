package geotrellis.data.geotiff

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

import scala.collection.mutable
import scala.annotation.switch

import scala.math.{ceil, min}

import geotrellis._

// TODO: compression?
// TODO: color?

/**
 * Class for writing GeoTIFF data to a given DataOutputStream.
 *
 * This class implements the basic TIFF spec [1] and also supports the GeoTIFF
 * extension tags. It is not a general purpose TIFF encoder (it doesn't support
 * renderings involving colors, multiband images, etc) but works well for
 * encoding raster data.
 *
 * It uses a single sample per pixel and encodes the data in one band. This
 * means that files using more than 8-bit samples will often not render
 * correctly in image programs like the Gimp. It also does not implement
 * compression yet.
 *
 * Future work may add compression options, as well as options related to the
 * color palette, multiple bands, etc.
 *
 * Encoders are not thread-safe and can only be used to write a single raster
 * to single output stream. This is a consequence of how reliant the TIFF
 * format is on embedding data addresses.
 *
 * [1] http://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf
 */
class Encoder(dos:DataOutputStream, raster:Raster, val settings:Settings) {
  val data = raster.toArray
  val re = raster.rasterExtent
  val cols = re.cols
  val rows = re.rows
  val e = re.extent

  /**
   * Here we do a bunch of calculations around strip size. We limit each strip
   * to 65K in size. Given that constraint, we can determine how many strips we
   * need, how large each strip is, how many rows per strip we'll write, etc.
   */

  final def bytesPerRow:Int = cols * bytesPerSample
  final def maxRowsPerStrip:Int = 65535 / bytesPerRow
  final def rowsPerStrip:Int = min(rows, maxRowsPerStrip)
  final def bytesPerStrip:Int = rowsPerStrip * bytesPerRow
  final def numStrips:Int = ceil((1.0 * rows) / rowsPerStrip).toInt
  final def leftOverRows:Int = rows % rowsPerStrip

  /**
   * ESRI compatibility changes the data we write out. It results in some extra
   * TIFF tags and many extra GeoTIFF tags.
   */
  final def esriCompat:Boolean = settings.esriCompat

  /**
   * Number of bits per sample. Should either be a multiple of 8, or evenly
   * divide 8 (i.e. 1, 2 or 4).
   */ 
  final def bitsPerSample:Int = settings.size.bits
  final def bytesPerSample:Int = settings.size.bits / 8

  /**
   * Type of samples, using numeric constants from the TIFF spec.
   */
  final def sampleFormat:Int = settings.format.kind

  /**
   * The number of TIFF tags to be written.
   *
   * If we update the writer to support additional TIFF tags this number needs
   * to be increased also. The writeTag calls in write are numbered to help
   * make this process easier.
   */
  final def numEntries = if (esriCompat) 21 else 19

  /**
   * The number of GeoTags to be written.
   *
   * If we write extra GeoTIFF tags this number needs to be increased.
   */
  final def numGeoTags = if (esriCompat) 21 else 4

  // we often need to "defer" writing data blocks, but keep track of how much
  // data would be written, to correctly compute future data block addresses.
  // we use 'todo' (backed by 'baos') to store this kind of data, and the
  // 'dataOffset' function to compute these addresses.
  val baos = new ByteArrayOutputStream()
  val todo = new DataOutputStream(baos)

  // The address we will start writing the image to.
  final def imageStartOffset:Int = 8

  // The addres we expect to see IFD information at.
  final def ifdOffset = imageStartOffset + img.size

  // The current address we will write (non-image) "data blocks" to.
  final def dataOffset = ifdOffset + 2 + (numEntries * 12) + 4 + baos.size

  // The byte array we will write all out 'strips' of image data to. We need
  // this to correctly compute addresses beyond the image (i.e. TIFF headers
  // and data blocks).
  val img = new ByteArrayOutputStream()

  // The number of bytes we've written to our 'dos'. Only used for debugging.
  var index:Int = 0

  // Used to immediately write values to our ultimate destination.
  def writeByte(value:Int) { dos.writeByte(value); index += 1 }
  def writeShort(value:Int) { dos.writeShort(value); index += 2 }
  def writeInt(value:Int) { dos.writeInt(value); index += 4 }
  def writeLong(value:Long) { dos.writeLong(value); index += 8 }
  def writeFloat(value:Float) { dos.writeFloat(value); index += 4 }
  def writeDouble(value:Double) { dos.writeDouble(value); index += 8 }

  // Used to write data blocks to our "deferred" byte array.
  def todoByte(value:Int) { todo.writeByte(value) }
  def todoShort(value:Int) { todo.writeShort(value) }
  def todoInt(value:Int) { todo.writeInt(value) }
  def todoLong(value:Long) { todo.writeLong(value) }
  def todoFloat(value:Float) { todo.writeFloat(value) }
  def todoDouble(value:Double) { todo.writeDouble(value) }

  // High-level function to defer writing geotiff tags.
  def todoGeoTag(tag:Int, loc:Int, count:Int, offset:Int) {
    todoShort(tag)
    todoShort(loc)
    todoShort(count)
    todoShort(offset)
  }

  // High-level function to write strings (char arrays). This will do immediate
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

  // High-level function to immediately write a TIFF tag. if value is an offset
  // then the user is responsible for making sure it is valid.
  def writeTag(tag:Int, typ:Int, count:Int, value:Int) {
    writeShort(tag)
    writeShort(typ)
    writeInt(count)
    if (count > 1) {
      writeInt(value)
    } else if (typ == Const.uint8 || typ == Const.sint8) {
      writeByte(value)
      writeByte(0)
      writeShort(0)
    } else if (typ == Const.uint16 || typ == Const.sint16) {
      writeShort(value)
      writeShort(0)
    } else {
      writeInt(value)
    }
  }

  // High-level function to immediately write a TIFF tag. if value is an offset
  // then the user is responsible for making sure it is valid.
  def writeTagDouble(tag:Int, typ:Int, value:Double) {
    writeShort(tag)
    writeShort(typ)
    writeInt(1)
    if (typ == Const.float32) {
      writeFloat(value.toFloat)
    } else {
      writeInt(dataOffset)
      todoDouble(value)
    }
  }

  /**
   * Immediately write a byte array to the output stream.
   */
  def writeByteArrayOutputStream(b:ByteArrayOutputStream) {
    b.writeTo(dos)
    index += b.size
  }

  /**
   * Render the raster data into strips, and return an array of file offsets
   * for each strip. The strips may (or may not) be compressed.
   */
  def renderImage():(Array[Int], Array[Int]) = settings.compression match {
    case Uncompressed => RawEncoder.render(this)
    case Lzw => LzwEncoder.render(this)
  }

  /**
   * Encodes the raster to GeoTIFF, and writes the data to the output stream.
   *
   * This method does not return a result; the result is written into the
   * output stream provided to Encoder's constructor. Many of the methods used
   * by write mutate the object, and Encoder is not thread-safe, so it's
   * important not to call this more than once.
   */
  def write() {
    // 0x0000: first 4 bytes of signature
    writeInt(0x4d4d002a)

    // render image (does not write output)
    val (stripOffsets, stripLengths) = renderImage()

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
    settings.compression match {
      case Lzw => writeTag(0x0103, Const.uint16, 1, 5)
      case Uncompressed => writeTag(0x0103, Const.uint16, 1, 1)
    }

    // 5. photometric interpretation, black is zero (1)
    writeTag(0x0106, Const.uint16, 1, 1)

    // 6. strip offsets (actual image data)
    val numStrips = stripOffsets.length
    if (numStrips == 1) {
      // if we have one strip, write it's address in the tag
      writeTag(0x0111, Const.uint32, 1, stripOffsets(0))
    } else {
      // if we have multiple strips, write each addr to a data block
      writeTag(0x0111, Const.uint32, numStrips, dataOffset)
      stripOffsets.foreach(addr => todoInt(addr))
    }

    // 7. 1 sample per pixel
    writeTag(0x0115, Const.uint16, 1, 1)

    // 8. 'rows' rows per strip
    writeTag(0x0116, Const.uint32, 1, rowsPerStrip)

    // 9. strip byte counts
    if (numStrips == 1) {
      writeTag(0x0117, Const.uint32, 1, stripLengths(0))
    } else {
      writeTag(0x0117, Const.uint32, numStrips, dataOffset)
      stripLengths.foreach(n => todoInt(n))
    }

    val kind = (settings.format, settings.size.bits) match {
      case (Floating, 32) => Const.float32
      case (Floating, 64) => Const.float64
      case (Signed, 8) => Const.sint8
      case (Signed, 16) => Const.sint16
      case (Signed, 32) => Const.sint32
      case (Unsigned, 8) => Const.sint8
      case (Unsigned, 16) => Const.sint16
      case (Unsigned, 32) => Const.sint32
      case tpl => sys.error("unsupported: %s" format tpl)
    }

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

    // 14. Differencing Predictor (1=none, 2=horizontal)
    settings.compression match {
      //case Lzw => writeTag(0x013D, Const.uint16, 1, 2)
      case Lzw => writeTag(0x013D, Const.uint16, 1, 1)
      case Uncompressed => writeTag(0x013D, Const.uint16, 1, 1)
    }

    // 15. sample format (1=unsigned, 2=signed, 3=floating point)
    writeTag(0x0153, Const.uint16, 1, sampleFormat)

    // 16. model pixel scale tag (points to doubles sX, sY, sZ with sZ = 0)
    writeTag(0x830e, Const.float64, 3, dataOffset)
    todoDouble(re.cellwidth)  // sx
    todoDouble(re.cellheight) // sy
    todoDouble(0.0)           // sz = 0.0

    // 17. model tie point (doubles I,J,K (grid) and X,Y,Z (geog) with K = Z = 0.0)
    writeTag(0x8482, Const.float64, 6, dataOffset)
    todoDouble(0.0)    // i
    todoDouble(0.0)    // j
    todoDouble(0.0)    // k = 0.0
    todoDouble(e.xmin) // x
    todoDouble(e.ymax) // y
    todoDouble(0.0)    // z = 0.0

    // 18. geo key directory tag (4 geotags each with 4 short values)
    writeTag(0x87af, Const.uint16, numGeoTags * 4, dataOffset)

    // write out GeoTags. this varies a lot depending on whether we are trying
    // to be compatible with ESRI (esriCompat is true) or whether we are just
    // going by the GeoTIFF spec (i.e. GDAL).
    if (esriCompat) {
      // ESRI actually defines the projection, etc, inline
      todoGeoTag(0x0001, 1, 2, numGeoTags) //  1. geotif 1.2, N more tags
      todoGeoTag(0x0400, 0, 1, 1)          //  2. projected data (1)
      todoGeoTag(0x0401, 0, 1, 1)          //  3. area data (1)
      todoGeoTag(0x0402, 0x87b1, 33, 0)    //  4. gt citation citation
      todoGeoTag(0x0800, 0, 1, 0x7fff)     //  5. user-defined geog type
      todoGeoTag(0x0801, 0x87b1, 151, 33)  //  6. geog citation
      todoGeoTag(0x0802, 0, 1, 0x7fff)     //  7. user-defined gcs type
      todoGeoTag(0x0806, 0, 1, 0x238e)     //  8. angular units: degree
      todoGeoTag(0x0808, 0, 1, 0x7fff)     //  9. user-defined ellipsoid geokey
      todoGeoTag(0x0809, 0x87b0, 1, 5)     // 10. ellipsoid geokey
      todoGeoTag(0x080a, 0x87b0, 1, 6)     // 11. semi major axis geokey
      todoGeoTag(0x080d, 0x87b0, 1, 7)     // 12. prime meridian geokey
      todoGeoTag(0x0c00, 0, 1, 3857)       // 13. gt projected cs type (3857)
      todoGeoTag(0x0c02, 0, 1, 0x7fff)     // 14. user-defined projection code
      todoGeoTag(0x0c03, 0, 1, 7)          // 15. mercator coordinate transform
      todoGeoTag(0x0c04, 0, 1, 0x2329)     // 16. linear unit size in meters
      todoGeoTag(0x0c08, 0x87b0, 1, 1)     // 17. nat origin long geokey
      todoGeoTag(0x0c09, 0x87b0, 1, 0)     // 18. nat origin lat geokey
      todoGeoTag(0x0c0a, 0x87b0, 1, 3)     // 19. false easting geokey
      todoGeoTag(0x0c0b, 0x87b0, 1, 4)     // 20. false northing geokey
      todoGeoTag(0x0c14, 0x87b0, 1, 2)     // 21. scale at nat origin geokey

      // 19. geotiff double params (tag only needed when esriCompat is true)
      writeTag(0x87b0, Const.float64, 8, dataOffset)
      todoDouble(0.0)       // nat origin lat
      todoDouble(0.0)       // nat origin lon
      todoDouble(1.0)       // scale at nat origin
      todoDouble(0.0)       // false easting
      todoDouble(0.0)       // false northing
      todoDouble(6378137.0) // ellipsoid
      todoDouble(6378137.0) // semi major axis
      todoDouble(0.0)       // prime meridian
      
      // 20. esri citation junk (tag is only needed when esriCompat is true)
      writeString(0x87b1, "PCS Name = WGS_1984_Web_Mercator|GCS Name = GCS_WGS_1984_Major_Auxiliary_Sphere|Datum = WGS_1984_Major_Auxiliary_Sphere|Ellipsoid = WGS_1984_Major_Auxiliary_Sphere|Primem = Greenwich||")

    } else {
      // GDAL only needs these 4 GeoTags
      todoGeoTag(0x0001, 1, 2, numGeoTags) // 1. geotif 1.2, N more tags
      todoGeoTag(0x0400, 0, 1, 2)          // 2. projected data (1)
      todoGeoTag(0x0401, 0, 1, 1)          // 3. area data (1)
      todoGeoTag(0x0800, 0, 1, 4326)       // 4. gt projected cs type (3857)
    }

    // 21. nodata as string (#19 if esriCompat is false)
    writeString(0xa481, settings.nodataString)

    // no more ifd entries
    writeInt(0)

    // write all our data blocks
    writeByteArrayOutputStream(baos)

    dos.flush()
  }
}

/**
 * The Encoder object provides several useful static methods for encoding 
 * raster data, which create Encoder instances on demand.
 */
object Encoder {

  /**
   * Encode raster as GeoTIFF and return an array of bytes.
   */
  def writeBytes(raster:Raster, settings:Settings) = {
    val baos = new ByteArrayOutputStream()
    val dos = new DataOutputStream(baos)
    val encoder = new Encoder(dos, raster, settings)
    encoder.write()
    baos.toByteArray
  }

  /**
   * Encode raster as GeoTIFF and write the data to the given path.
   */
  def writePath(path:String, raster:Raster, settings:Settings) {
    val fos = new FileOutputStream(new File(path))
    val dos = new DataOutputStream(fos)
    val encoder = new Encoder(dos, raster, settings)
    encoder.write()
    fos.close()
  }
}
