package geotrellis.raster.io.geotiff.reader

import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.utils._
import spire.syntax.cfor._

import java.nio.{ ByteBuffer, ByteOrder }

object TagsReader {
  def read(path: String): Tags = read(Filesystem.slurp(path))

  def read(bytes: Array[Byte]): Tags = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)

    // Set byteBuffer position
    byteBuffer.position(0)

    // set byte ordering
    (byteBuffer.get.toChar, byteBuffer.get.toChar) match {
      case ('I', 'I') => byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
      case ('M', 'M') => byteBuffer.order(ByteOrder.BIG_ENDIAN)
      case _ => throw new MalformedGeoTiffException("incorrect byte order")
    }

    // Validate GeoTiff identification number
    val geoTiffIdNumber = byteBuffer.getChar
    if ( geoTiffIdNumber != 42)
      throw new MalformedGeoTiffException(s"bad identification number (must be 42, was $geoTiffIdNumber)")

    val tagsStartPosition = byteBuffer.getInt

    read(byteBuffer, tagsStartPosition)
  }

  def read(byteBuffer: ByteBuffer, tagsStartPosition: Int): Tags = {
    byteBuffer.position(tagsStartPosition)

    val tagCount = byteBuffer.getShort

    // Read the tags.
    var tags = Tags()

    // Need to read geo tags last, relies on other tags already being read in.
    var geoTags: Option[TagMetadata] = None

    cfor(0)(_ < tagCount, _ + 1) { i =>
      val tagMetaData =
        TagMetadata(
          byteBuffer.getUnsignedShort,
          byteBuffer.getUnsignedShort,
          byteBuffer.getInt,
          byteBuffer.getInt
        )

      if (tagMetaData.tag == codes.TagCodes.GeoKeyDirectoryTag)
        geoTags = Some(tagMetaData)
      else 
        tags = TagReader.read(byteBuffer, tags, tagMetaData)
    }

    geoTags match {
      case Some(t) => tags = TagReader.read(byteBuffer, tags, t)
      case None =>
    }

    tags
  }
}
