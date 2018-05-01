package geotrellis.spark.io.file.conf

import geotrellis.spark.io.hadoop.conf.CamelCaseConfig
import geotrellis.spark.util.threadsFromString

case class FileCollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class FileRDDConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}

case class FileThreadsConfig(
  collection: FileCollectionConfig = FileCollectionConfig(),
  rdd: FileRDDConfig = FileRDDConfig()
)

case class FileConfig(threads: FileThreadsConfig = FileThreadsConfig())

object FileConfig extends CamelCaseConfig {
  lazy val conf: FileConfig = pureconfig.loadConfigOrThrow[FileConfig]("geotrellis.file")
  implicit def fileConfigToClass(obj: FileConfig.type): FileConfig = conf
}
