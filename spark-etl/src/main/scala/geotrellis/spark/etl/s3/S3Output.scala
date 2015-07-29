package geotrellis.spark.etl.s3

import geotrellis.spark.etl.OutputPlugin

trait S3Output extends OutputPlugin {
  val name = "s3"
  val requiredKeys = Array("bucket", "key")
}
