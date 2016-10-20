package geotrellis.spark.io.s3.util

import geotrellis.util.BytesStreamer
import geotrellis.spark.io.s3._

import com.amazonaws.services.s3.model._

/**
 * This class extends [[BytesStreamer]] by reading chunks out of a GeoTiff on the
 * AWS S3 server.
 *
 * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
 * @param client: The [[S3Client]] that retrieves the data.
 * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
 * @return A new instance of S3BytesStreamer.
 */
class S3BytesStreamer(
  request: GetObjectRequest,
  client: S3Client,
  val chunkSize: Int) extends BytesStreamer {

  val metadata: ObjectMetadata =
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

/** The companion object of [[S3BytesStreamer]] */
object S3BytesStreamer {

  /**
   * Returns a new instance of S3BytesStreamer.
   *
   * @param bucket: A string that is the name of the bucket.
   * @param key: A string that is the path to the GeoTiff.
   * @param client: The [[S3Client]] that retrieves the data.
   * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
   * @return A new instance of S3BytesStreamer.
   */
  def apply(bucket: String, key: String, client: S3Client, chunkSize: Int): S3BytesStreamer =
    new S3BytesStreamer(new GetObjectRequest(bucket, key), client, chunkSize)

  /**
   * Returns a new isntance of S3BytesStreamer.
   *
   * @param request: A [[GetObjectRequest]] of the desired GeoTiff.
   * @param client: The [[S3Client]] that retrieves the data.
   * @param chunkSize: An Int that specifies how many bytes should be read in at a time.
   * @return A new instance of S3BytesStreamer.
   */
  def apply(request: GetObjectRequest, client: S3Client, chunkSize: Int): S3BytesStreamer =
    new S3BytesStreamer(request, client, chunkSize)
}
