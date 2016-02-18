package geotrellis.spark.etl.s3

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.io.s3.S3AttributeStore


trait S3Output[K, V, M] extends OutputPlugin[K, V, M] {
  val name = "s3"
  val requiredKeys = Array("bucket", "key")

  def attributes(props: Map[String, String]) = S3AttributeStore(props("bucket"), props("key"))
}
