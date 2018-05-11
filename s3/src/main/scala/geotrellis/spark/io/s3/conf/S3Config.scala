package geotrellis.spark.io.s3.conf

import geotrellis.spark.io.hadoop.conf.CamelCaseConfig
import geotrellis.spark.util.threadsFromString

case class S3CollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class S3RDDConfig(write: String = "default", read: String = "default") {
  def readThreads: Int = threadsFromString(read)
  def writeThreads: Int = threadsFromString(write)
}

case class S3ThreadsConfig(
  collection: S3CollectionConfig = S3CollectionConfig(),
  rdd: S3RDDConfig = S3RDDConfig()
)

case class S3RDDReadWindowSize(windowSize: Int = 1024)
case class S3RDDReadConfig(read: S3RDDReadWindowSize = S3RDDReadWindowSize())

case class S3Config(threads: S3ThreadsConfig = S3ThreadsConfig(), rdd: S3RDDReadConfig = S3RDDReadConfig())

object S3Config extends CamelCaseConfig {
  lazy val conf: S3Config = pureconfig.loadConfigOrThrow[S3Config]("geotrellis.s3")
  implicit def s3ConfigToClass(obj: S3Config.type): S3Config = conf
}
