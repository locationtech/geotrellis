package geotrellis.spark.etl.s3

import geotrellis.spark.etl.{EtlJob, OutputPlugin}
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io.s3.S3AttributeStore

trait S3Output[K, V, M] extends OutputPlugin[K, V, M] {
  val name = "s3"

  def attributes(job: EtlJob) = S3AttributeStore(job.outputProps("bucket"), job.outputProps("key"))
}
