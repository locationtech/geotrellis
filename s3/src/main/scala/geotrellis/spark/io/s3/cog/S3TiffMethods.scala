package geotrellis.spark.io.s3.cog

import geotrellis.raster.{GridBounds, MultibandTile, Tile}
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffMultibandTile, GeoTiffTile}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.spark.io.cog.TiffMethods
import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.StreamingByteReader

import com.amazonaws.services.s3.AmazonS3URI

import java.net.URI

trait S3TiffMethods {
  implicit val tiffMethods = new TiffMethods[Tile] {
    def getS3Client: () => S3Client = () => S3Client.DEFAULT

    def readTiff(uri: URI, index: Int): GeoTiff[Tile] = {
      val auri = new AmazonS3URI(uri)

      val reader = StreamingByteReader(
        S3RangeReader(
          bucket = auri.getBucket,
          key    = auri.getKey,
          client = getS3Client()
        )
      )

      val tiff = GeoTiffReader.readSingleband(reader, false, true)

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
      val auri = new AmazonS3URI(uri)

      val reader = StreamingByteReader(
        S3RangeReader(
          bucket = auri.getBucket,
          key    = auri.getKey,
          client = getS3Client()
        )
      )

      val info =
        readGeoTiffInfo(
          byteReader         = reader,
          decompress         = false,
          streaming          = true,
          withOverviews      = true,
          byteReaderExternal = None
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
      val auri = new AmazonS3URI(uri)

      val reader = StreamingByteReader(
        S3RangeReader(
          bucket = auri.getBucket,
          key    = auri.getKey,
          client = getS3Client()
        )
      )

      val tiff = GeoTiffReader.readMultiband(reader, false, true)

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
      val auri = new AmazonS3URI(uri)

      val reader = StreamingByteReader(
        S3RangeReader(
          bucket = auri.getBucket,
          key    = auri.getKey,
          client = getS3Client()
        )
      )

      val info =
        readGeoTiffInfo(
          byteReader         = reader,
          decompress         = false,
          streaming          = true,
          withOverviews      = true,
          byteReaderExternal = None
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
