package geotrellis.spark.io.s3.cog

import java.net.URI

import com.amazonaws.services.s3.AmazonS3URI
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.{ByteReader, StreamingByteReader}

trait Implicits extends Serializable {
  implicit val s3SinglebandCOGRDDReader = new S3COGRDDReader[Tile] {
    def getS3Client: () => S3Client = () => S3Client.DEFAULT

    override def readTiff(uri: URI) = {
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
  }
}
