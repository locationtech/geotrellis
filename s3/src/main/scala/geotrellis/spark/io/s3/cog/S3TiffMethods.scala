package geotrellis.spark.io.s3.cog

import geotrellis.raster.{CellGrid, GridBounds, MultibandTile, Tile}
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import java.net.URI

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.spark.io.cog.TiffMethods
import geotrellis.spark.io.s3.S3Client

trait S3TiffMethods {
  implicit val tiffMethods = new TiffMethods[Tile] {
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

    def tileTiff[K](tiff: GeoTiff[Tile], gridBounds: GridBounds): Tile = {
      tiff.tile match {
        case gtTile: GeoTiffTile => gtTile.crop(gridBounds)
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }
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

  implicit val multibandTiffMethods = new TiffMethods[MultibandTile] {
    def getS3Client: () => S3Client = () => S3Client.DEFAULT

    def readTiff(uri: URI, index: Int): GeoTiff[MultibandTile] = {
      val (reader, ovrReader) = S3COGRDDReader.getReaders(uri, getS3Client)

      val tiff =
        GeoTiffReader
          .readMultiband(
            byteReader         = reader,
            decompress         = false,
            streaming          = true,
            withOverviews      = true,
            byteReaderExternal = ovrReader
          )

      if(index < 0) tiff
      else tiff.getOverview(index)
    }

    def tileTiff[K](tiff: GeoTiff[MultibandTile], gridBounds: GridBounds): MultibandTile = {
      tiff.tile match {
        case gtTile: GeoTiffMultibandTile => gtTile.crop(gridBounds)
        case _ => throw new UnsupportedOperationException("Can be applied to a GeoTiffTile only.")
      }
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
        GeoTiffReader.geoTiffMultibandTile(info)

      val tiff =
        if(index < 0) geoTiffTile
        else geoTiffTile.overviews(index)

      val func: (Int, Int) => GridBounds = { (col, row) => tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row)) }

      func
    }
  }
}
