package geotrellis.spark.io.hadoop.cog

import geotrellis.raster.io.geotiff.reader.{GeoTiffReader, TiffTagsReader}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.{GridBounds, MultibandTile, Tile}
import geotrellis.spark.io.cog.TiffMethods

import spray.json._

import java.net.URI
import java.nio.ByteBuffer

trait HadoopTiffMethods {
  implicit val hadoopTiffMethods: TiffMethods[Tile] with HadoopCOGBackend =
    new TiffMethods[Tile] with HadoopCOGBackend {
      def readTiff(bytes: Array[Byte], index: Int): GeoTiff[Tile] = {
        val tiff = GeoTiffReader.readSingleband(bytes)

        if (index < 0) tiff
        else tiff.getOverview(index)
      }

      def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
        val tiff =
          GeoTiffReader
            .readSingleband(
              byteReader = getReader(uri),
              decompress = false,
              streaming = true,
              withOverviews = true,
              byteReaderExternal = None
            )

        if (index < 0) tiff
        else tiff.getOverview(index)
      }

      def cropTiff(tiff: GeoTiff[Tile], gridBounds: GridBounds): Tile = {
        tiff.tile match {
          case gtTile: GeoTiffTile => gtTile.crop(gridBounds)
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getSegmentGridBounds(bytes: Array[Byte], index: Int): (Int, Int) => GridBounds = {
        val info =
          readGeoTiffInfo(
            byteReader = ByteBuffer.wrap(bytes),
            decompress = false,
            streaming = true,
            withOverviews = true,
            byteReaderExternal = None
          )

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if (index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val info = getGeoTiffInfo(uri)

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }

      def getGeoTiffInfo(uri: URI): GeoTiffReader.GeoTiffInfo =
        readGeoTiffInfo(
          byteReader = getReader(uri),
          decompress = false,
          streaming = true,
          withOverviews = true,
          byteReaderExternal = None
        )

      def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: Map[GridBounds, K]): Vector[(K, Tile)] = {
        tiff.tile match {
          case gtTile: GeoTiffTile =>
            gtTile
              .crop(gridBounds.keys.toSeq)
              .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
              .toVector
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getKey[K: JsonFormat](uri: URI): K =
        TiffTagsReader
          .read(getReader(uri))
          .tags
          .headTags("GT_KEY")
          .parseJson
          .convertTo[K]
    }

  implicit val hadoopMultibandTiffMethods: TiffMethods[MultibandTile] with HadoopCOGBackend =
    new TiffMethods[MultibandTile] with HadoopCOGBackend {
      override def readTiff(bytes: Array[Byte], index: Int): GeoTiff[MultibandTile] = {
        val tiff = GeoTiffReader.readMultiband(bytes)

        if (index < 0) tiff
        else tiff.getOverview(index)
      }

      def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
        val tiff =
          GeoTiffReader
            .readMultiband(
              byteReader = getReader(uri),
              decompress = false,
              streaming = true,
              withOverviews = true,
              byteReaderExternal = None
            )

        if (index < 0) tiff
        else tiff.getOverview(index)
      }

      def cropTiff(tiff: GeoTiff[MultibandTile], gridBounds: GridBounds): MultibandTile = {
        tiff.tile match {
          case gtTile: GeoTiffMultibandTile => gtTile.crop(gridBounds)
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val info = getGeoTiffInfo(uri)

        val geoTiffTile =
          GeoTiffReader.geoTiffMultibandTile(info)

        val tiff =
          if (index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }

      def getGeoTiffInfo(uri: URI): GeoTiffReader.GeoTiffInfo =
        readGeoTiffInfo(
          byteReader = getReader(uri),
          decompress = false,
          streaming = true,
          withOverviews = true,
          byteReaderExternal = None
        )

      def tileTiff[K](tiff: GeoTiff[MultibandTile], gridBounds: Map[GridBounds, K]): Vector[(K, MultibandTile)] = {
        tiff.tile match {
          case gtTile: GeoTiffMultibandTile =>
            gtTile
              .crop(gridBounds.keys.toSeq)
              .flatMap { case (k, v) => gridBounds.get(k).map(i => i -> v) }
              .toVector
          case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
        }
      }

      def getKey[K: JsonFormat](uri: URI): K =
        TiffTagsReader
          .read(getReader(uri))
          .tags
          .headTags("GT_KEY")
          .parseJson
          .convertTo[K]

      def getSegmentGridBounds(bytes: Array[Byte], index: Int): (Int, Int) => GridBounds = {
        val info =
          readGeoTiffInfo(
            byteReader = ByteBuffer.wrap(bytes),
            decompress = false,
            streaming = true,
            withOverviews = true,
            byteReaderExternal = None
          )

        val geoTiffTile =
          GeoTiffReader.geoTiffMultibandTile(info)

        val tiff =
          if (index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }
}
