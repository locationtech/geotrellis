package geotrellis.spark.io.accumulo.conf

import geotrellis.spark.io.hadoop.conf.CamelCaseConfig
import geotrellis.spark.util.threadsFromString

case class AccumuloCollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class AccumuloRDDConfig(write: String = "default") {
  def writeThreads: Int = threadsFromString(write)
}

case class AccumuloThreadsConfig(
  collection: AccumuloCollectionConfig = AccumuloCollectionConfig(),
  rdd: AccumuloRDDConfig = AccumuloRDDConfig()
)

case class AccumuloConfig(
  catalog: String = "metadata",
  threads: AccumuloThreadsConfig = AccumuloThreadsConfig()
)

object AccumuloConfig extends CamelCaseConfig {
  lazy val conf: AccumuloConfig = pureconfig.loadConfigOrThrow[AccumuloConfig]("geotrellis.accumulo")
  implicit def accumuloConfigToClass(obj: AccumuloConfig.type): AccumuloConfig = conf
}
