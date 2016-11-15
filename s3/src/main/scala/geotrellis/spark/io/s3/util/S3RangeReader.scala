package geotrellis.spark.io.s3.util

import geotrellis.spark.io.s3._
import geotrellis.util.RangeReader

import com.amazonaws.services.s3.model._

/**
 * This class extends [[RangeReader]] by reading chunks out of a GeoTiff on the
 * AWS S3 server.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[S3Client]] that retrieves the data.
 * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
 * @return A new instance of S3RangeReader.
 */
class S3RangeReader(
  request: GetObjectRequest,
  client: S3Client) extends RangeReader {

  val metadata: ObjectMetadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  val totalLength: Long = metadata.getContentLength

  def readClippedRange(start: Long, length: Int): Array[Byte] =
    client.readRange(start, start + length, request)
}

/** The companion object of [[S3RangeReader]] */
object S3RangeReader {
  /**
   * Returns a new instance of S3RangeReader.
   *
   * @param bucket: A string that is the name of the bucket.
   * @param key: A string that is the path to the GeoTiff.
   * @param client: The [[S3Client]] that retrieves the data.
   * @return A new instance of S3RangeReader.
   */
  def apply(bucket: String, key: String, client: S3Client): S3RangeReader =
    apply(new GetObjectRequest(bucket, key), client)

  /**
   * Returns a new instance of S3RangeReader.
   *
   * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
   * @param client: The [[S3Client]] that retrieves the data.
   * @return A new instance of S3RangeReader.
   */
  def apply(request: GetObjectRequest, client: S3Client): S3RangeReader =
    new S3RangeReader(request, client)
}
