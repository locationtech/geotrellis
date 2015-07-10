package geotrellis.spark.etl.s3

import geotrellis.spark.etl.SinkPlugin

trait S3Sink extends SinkPlugin {
  val name = "s3"
  val requiredKeys = Array("bucket", "key")
}
