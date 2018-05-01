package geotrellis.spark.io.cassandra.conf

import geotrellis.spark.io.hadoop.conf.CamelCaseConfig
import geotrellis.spark.util._

case class CassandraCollectionConfig(read: String = "default") {
  def readThreads: Int = threadsFromString(read)
}
case class CassandraRDDConfig(write: String = "default", read: String = "default") {
  def readThreads: Int = threadsFromString(read)
  def writeThreads: Int = threadsFromString(write)
}

case class CassandraThreadsConfig(
  collection: CassandraCollectionConfig = CassandraCollectionConfig(),
  rdd: CassandraRDDConfig = CassandraRDDConfig()
)

case class CassandraConfig(
  port: Int = 9042,
  catalog: String = "metadata",
  keyspace: String = "geotrellis",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1,
  localDc: String = "datacenter1",
  usedHostsPerRemoteDc: Int = 0,
  allowRemoteDCsForLocalConsistencyLevel: Boolean = false,
  threads: CassandraThreadsConfig = CassandraThreadsConfig()
)

object CassandraConfig extends CamelCaseConfig {
  lazy val conf: CassandraConfig = pureconfig.loadConfigOrThrow[CassandraConfig]("geotrellis.cassandra")
  implicit def cassandraConfigToClass(obj: CassandraConfig.type): CassandraConfig = conf
}
