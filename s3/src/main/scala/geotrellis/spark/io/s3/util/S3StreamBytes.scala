package geotrellis.spark.io.s3.util

import geotrellis.util.StreamBytes
import geotrellis.spark.io.s3._

import com.amazonaws.services.s3.model._

/**
 * This class extends [[StreamBytes]] by reading chunks out of a GeoTiff on the
 * AWS S3 server.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[AmazonS3Client]] that retrieves the data.
 * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
 * @return A new instance of S3StreamBytes.
 */
class S3StreamBytes(
  request: GetObjectRequest,
  client: AmazonS3Client,
  val chunkSize: Int) extends StreamBytes {

  def metadata: ObjectMetadata =
    client.getObjectMetadata(request.getBucketName, request.getKey)

  def objectLength: Long = metadata.getContentLength
  
  def getArray(start: Long, length: Long): Array[Byte] = {
    val chunk: Long =
      if (!passedLength(length + start))
        length
      else
        objectLength - start

    client.readRange(start, start + chunk, request)
  }
}

/** The companion object of [[S3StreamBytes]] */
object S3StreamBytes {

  /**
   * Returns a new isntance of S3StreamBytes.
   *
   * @param bucket: A string that is the name of the bucket.
   * @param key: A string that is the path to the GeoTiff.
   * @param client: The [[AmazonS3Client]] that retrieves the data.
   * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
   * @return A new instance of S3StreamBytes.
   */
  def apply(bucket: String, key: String, client: AmazonS3Client, chunkSize: Int): S3StreamBytes =
    new S3StreamBytes(new GetObjectRequest(bucket, key), client, chunkSize)

  /**
   * Returns a new isntance of S3StreamBytes.
   *
   * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
   * @param client: The [[AmazonS3Client]] that retrieves the data.
   * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
   * @return A new instance of S3StreamBytes.
   */
  def apply(request: GetObjectRequest, client: AmazonS3Client, chunkSize: Int): S3StreamBytes =
    new S3StreamBytes(request, client, chunkSize)
}
