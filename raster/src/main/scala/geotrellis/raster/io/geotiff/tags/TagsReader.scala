package geotrellis.raster.io.geotiff.tags

import geotrellis.raster.io.geotiff.utils._
import spire.syntax.cfor._

import java.nio.ByteBuffer

object TagsReader {
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
