package geotrellis.spark.etl.s3

import geotrellis.spark.etl.OutputPlugin
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io.s3.S3AttributeStore

trait S3Output[K, V, M] extends OutputPlugin[K, V, M] {
  val name = "s3"

  def attributes(conf: EtlConf) = S3AttributeStore(conf.outputProps("bucket"), conf.outputProps("key"))
}
