package geotrellis.store.s3.util

import geotrellis.store.s3._
import geotrellis.util.RangeReaderProvider

import software.amazon.awssdk.services.s3.S3Client

import java.net.URI

class S3RangeReaderProvider extends RangeReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "s3") true else false
    case null => false
  }

  def rangeReader(uri: URI): S3RangeReader =
    rangeReader(uri, S3ClientProducer.get())

  def rangeReader(uri: URI, s3Client: S3Client): S3RangeReader = {
    val s3Uri = new AmazonS3URI(uri)
    val prefix = Option(s3Uri.getKey()).getOrElse("")

    S3RangeReader(s3Uri.getBucket(), prefix, s3Client)
  }
}
