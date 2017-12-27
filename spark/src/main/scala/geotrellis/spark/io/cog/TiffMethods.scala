package geotrellis.spark.io.cog

import geotrellis.raster.{CellGrid, GridBounds, MultibandTile, Tile}
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.util.ByteReader

import spray.json._
import java.nio.ByteBuffer

trait TiffMethods[V <: CellGrid] {
  def tileTiff[K](tiff: GeoTiff[V], gridBounds: Map[GridBounds, K]): Vector[(K, V)]

  def readTiff(byteReader: ByteReader, index: Int): GeoTiff[V]
  def readTiff(bytes: Array[Byte], index: Int): GeoTiff[V] =
    readTiff(ByteBuffer.wrap(bytes), index)

  def cropTiff(tiff: GeoTiff[V], gridBounds: GridBounds): V

  def getSegmentGridBounds(byteReader: ByteReader, index: Int): (Int, Int) => GridBounds
  def getSegmentGridBounds(bytes: Array[Byte], index: Int): (Int, Int) => GridBounds =
    getSegmentGridBounds(ByteBuffer.wrap(bytes), index)

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
    def readTiff(byteReader: ByteReader, index: Int): GeoTiff[Tile] = {
      val tiff = GeoTiffReader.readSingleband(byteReader, false, true)

      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def cropTiff(tiff: GeoTiff[Tile], gridBounds: GridBounds): Tile =
      tiff.tile match {
        case gtTile: GeoTiffTile => gtTile.crop(gridBounds)
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }

    def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: Map[GridBounds, K]): Vector[(K, Tile)] =
      tiff.tile match {
        case gtTile: GeoTiffTile =>
          gtTile
            .crop(gridBounds.keys.toSeq)
            .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
            .toVector
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }

    def getSegmentGridBounds(byteReader: ByteReader, index: Int): (Int, Int) => GridBounds = {
      val info = getGeoTiffInfo(byteReader)

      val geoTiffTile =
        GeoTiffReader.geoTiffSinglebandTile(info)

      val tiff =
        if(index < 0) geoTiffTile
        else geoTiffTile.overviews(index)

      val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

      func
    }
  }

  implicit val multibandTiffMethods: TiffMethods[MultibandTile] = new TiffMethods[MultibandTile] {
    def readTiff(byteReader: ByteReader, index: Int): GeoTiff[MultibandTile] = {
      val tiff = GeoTiffReader.readMultiband(byteReader, false, true)

      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def cropTiff(tiff: GeoTiff[MultibandTile], gridBounds: GridBounds): MultibandTile =
      tiff.tile match {
        case gtTile: GeoTiffMultibandTile => gtTile.crop(gridBounds)
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }

    def tileTiff[K](tiff: GeoTiff[MultibandTile], gridBounds: Map[GridBounds, K]): Vector[(K, MultibandTile)] =
      tiff.tile match {
        case gtTile: GeoTiffMultibandTile =>
          gtTile
            .crop(gridBounds.keys.toSeq)
            .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
            .toVector
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }

    def getSegmentGridBounds(byteReader: ByteReader, index: Int): (Int, Int) => GridBounds = {
      val info = getGeoTiffInfo(byteReader)

      val geoTiffTile =
        GeoTiffReader.geoTiffMultibandTile(info)

      val tiff =
        if(index < 0) geoTiffTile
        else geoTiffTile.overviews(index)

      val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

      func
    }
  }
}
