package geotrellis.spark.io.hadoop.conf

import geotrellis.spark.util.threadsFromString

case class HadoopCollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class HadoopRDDConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}

case class HadoopThreadsConfig(
  collection: HadoopCollectionConfig = HadoopCollectionConfig(),
  rdd: HadoopRDDConfig = HadoopRDDConfig()
)

case class HadoopConfig(threads: HadoopThreadsConfig = HadoopThreadsConfig())

object HadoopConfig extends CamelCaseConfig {
  lazy val conf: HadoopConfig = pureconfig.loadConfigOrThrow[HadoopConfig]("geotrellis.hadoop")
  implicit def hadoopConfigToClass(obj: HadoopConfig.type): HadoopConfig = conf
}
