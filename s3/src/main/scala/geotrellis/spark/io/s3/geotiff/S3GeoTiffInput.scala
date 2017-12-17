package geotrellis.spark.io.s3.geotiff

import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.spark.io.hadoop.geotiff.GeoTiffMetadata
import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.StreamingByteReader

import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model.ListObjectsRequest

import java.net.URI

object S3GeoTiffInput {

  /**
    * Returns a list of URIs matching given regexp
    */
  def list(
    name: String,
    uri: URI,
    pattern: String,
    recursive: Boolean = true,
    getS3Client: () => S3Client = () => S3Client.DEFAULT): List[GeoTiffMetadata] = {
    val s3Client = getS3Client()
    val s3Uri = new AmazonS3URI(uri)
    val regexp = pattern.r

    val objectRequest = (new ListObjectsRequest)
      .withBucketName(s3Uri.getBucket)
      .withPrefix(s3Uri.getKey)

    // Avoid digging into a deeper directory
    if (!recursive) objectRequest.withDelimiter("/")

    s3Client.listKeys(objectRequest)
      .flatMap { key =>
        key match {
          case regexp(_*) => Some(new AmazonS3URI(s"s3://${s3Uri.getBucket}/${key}"))
          case _ => None
        }
      }
      .map { auri =>
        val tiffTags = TiffTagsReader.read(StreamingByteReader(
          S3RangeReader(
            bucket = auri.getBucket,
            key = auri.getKey,
            client = getS3Client()
          )
        ))

        GeoTiffMetadata(tiffTags.extent, tiffTags.crs, name, new URI(auri.toString))
      }
      .toList
  }
}
