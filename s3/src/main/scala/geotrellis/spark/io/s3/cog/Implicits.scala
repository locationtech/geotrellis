package geotrellis.spark.io.s3.cog

import geotrellis.raster.{GridBounds, Tile}
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.spark.io.s3.S3Client

import java.net.URI

trait Implicits extends Serializable {
  implicit val s3SinglebandCOGRDDReader: S3COGRDDReader[Tile] =
    new S3COGRDDReader[Tile] {
      def getS3Client: () => S3Client = () => S3Client.DEFAULT

      def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
        val (reader, ovrReader) = S3COGRDDReader.getReaders(uri, getS3Client)

        val tiff =
          GeoTiffReader
            .readSingleband(
              byteReader         = reader,
              decompress         = false,
              streaming          = true,
              withOverviews      = true,
              byteReaderExternal = ovrReader
            )

        if(index < 0) tiff
        else tiff.getOverview(index)
      }

      def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
        val (reader, ovrReader) = S3COGRDDReader.getReaders(uri, getS3Client)

        val info =
          readGeoTiffInfo(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = ovrReader
          )

        val geoTiffTile =
          GeoTiffReader.geoTiffSinglebandTile(info)

        val tiff =
          if(index < 0) geoTiffTile
          else geoTiffTile.overviews(index)

        val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

        func
      }
    }
}
