package geotrellis.spark.io.s3

import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.{ByteReader, StreamingByteReader}

import com.amazonaws.services.s3.AmazonS3URI

import java.net.URI

package object cog {
  def s3PathExists(str: String, s3Client: S3Client): Boolean = {
    val arr = str.split("/").filterNot(_.isEmpty)
    val bucket = arr.head
    val prefix = arr.tail.mkString("/")
    s3Client.doesObjectExist(bucket, prefix)
  }

  def byteReader(uri: URI, s3Client: S3Client): ByteReader = {
    val auri = new AmazonS3URI(uri)

    StreamingByteReader(
      S3RangeReader(
        bucket = auri.getBucket,
        key    = auri.getKey,
        client = s3Client
      )
    )
  }
}
