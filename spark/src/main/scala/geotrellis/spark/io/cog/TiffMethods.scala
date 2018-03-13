package geotrellis.spark.io.cog

import geotrellis.raster.{CellGrid, GridBounds, MultibandTile, Tile}
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.util.ByteReader

import spray.json._
import java.nio.ByteBuffer

trait TiffMethods[V <: CellGrid] extends Serializable {
  def readEntireTiff(byteReader: ByteReader): GeoTiff[V]
  def readTiff(byteReader: ByteReader, index: Int): GeoTiff[V]
  def readTiff(bytes: Array[Byte], index: Int): GeoTiff[V] =
    readTiff(ByteBuffer.wrap(bytes), index)

  def getGeoTiffInfo(byteReader: ByteReader): GeoTiffReader.GeoTiffInfo =
    GeoTiffReader.readGeoTiffInfo(
      byteReader         = byteReader,
      decompress         = false,
      streaming          = true,
      withOverviews      = true,
      byteReaderExternal = None
    )

  def getKey[K: JsonFormat](byteReader: ByteReader): K =
    TiffTagsReader
      .read(byteReader)
      .tags
      .headTags(GTKey)
      .parseJson
      .convertTo[K]
}

trait TiffMethodsImplicits {
  implicit val singlebandTiffMethods: TiffMethods[Tile] = new TiffMethods[Tile] {
    def readEntireTiff(byteReader: ByteReader): GeoTiff[Tile] =
      GeoTiffReader
        .readSingleband(byteReader, false, true)

    def readTiff(byteReader: ByteReader, index: Int): GeoTiff[Tile] =
      GeoTiffReader
        .readSingleband(byteReader, false, true)
        .getOverview(index)
  }

  implicit val multibandTiffMethods: TiffMethods[MultibandTile] = new TiffMethods[MultibandTile] {
    def readEntireTiff(byteReader: ByteReader): GeoTiff[MultibandTile] =
      GeoTiffReader
        .readMultiband(byteReader, false, true)

    def readTiff(byteReader: ByteReader, index: Int): GeoTiff[MultibandTile] =
      GeoTiffReader
        .readMultiband(byteReader, false, true)
        .getOverview(index)
  }
}
