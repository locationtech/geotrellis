package geotrellis.spark.io.s3.cog

import java.net.URI

import com.amazonaws.services.s3.AmazonS3URI
import geotrellis.raster.{GridBounds, Tile}
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffTile}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.readGeoTiffInfo
import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.{ByteReader, StreamingByteReader}

trait Implicits extends Serializable {
  implicit val s3SinglebandCOGRDDReader = new S3COGRDDReader[Tile] {
    def getS3Client: () => S3Client = () => S3Client.DEFAULT

    def readTiff(uri: URI): GeoTiff[Tile] = {
      val auri = new AmazonS3URI(uri)
      val (bucket, key) = auri.getBucket -> auri.getKey
      val ovrKey = s"$key.ovr"
      val ovrReader: Option[ByteReader] =
        if (getS3Client().doesObjectExist(bucket, ovrKey)) Some(S3RangeReader(bucket, ovrKey, getS3Client())) else None

      GeoTiffReader
        .readSingleband(
          StreamingByteReader(
            S3RangeReader(
              bucket = bucket,
              key = key,
              client = getS3Client()
            )
          ),
          false,
          true,
          true,
          ovrReader
        )
    }

    def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds = {
      val auri = new AmazonS3URI(uri)
      val (bucket, key) = auri.getBucket -> auri.getKey
      val ovrKey = s"$key.ovr"
      val ovrReader: Option[ByteReader] =
        if (getS3Client().doesObjectExist(bucket, ovrKey)) Some(S3RangeReader(bucket, ovrKey, getS3Client())) else None

      val tiffReader =
        StreamingByteReader(
          S3RangeReader(
            bucket = bucket,
            key = key,
            client = getS3Client()
          )
        )

      val info =
        readGeoTiffInfo(
          tiffReader,
          false,
          true,
          true,
          ovrReader
        )

      val geoTiffTile =
        GeoTiffReader.geoTiffSinglebandTile(info)

      val tiff =
        if(index < 0) geoTiffTile
        else geoTiffTile.overviews(index)

      def res(col: Int, row: Int): GridBounds =
        tiff.getGridBounds(tiff.segmentLayout.getSegmentIndex(col, row))

      res _
    }

  }
}
