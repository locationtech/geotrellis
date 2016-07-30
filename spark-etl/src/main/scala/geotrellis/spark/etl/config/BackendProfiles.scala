package geotrellis.spark.etl.config

import geotrellis.spark.io.accumulo.AccumuloInstance
import geotrellis.spark.io.hbase.HBaseInstance
import geotrellis.spark.io.cassandra.{BaseCassandraInstance, Cassandra}

import org.apache.accumulo.core.client.security.tokens.PasswordToken

sealed trait BackendProfile {
  val name: String
  def `type`: String
}

case class HadoopProfile(name: String) extends BackendProfile { def `type` = HadoopType.name }
case class S3Profile(name: String, partitionsCount: Option[Int] = None) extends BackendProfile { def `type` = S3Type.name }
case class CassandraProfile(name: String, hosts: String, user: String, password: String,
                     replicationStrategy: String = Cassandra.cfg.getString("replicationStrategy"),
                     replicationFactor: Int = Cassandra.cfg.getInt("replicationFactor"),
                     localDc: String = Cassandra.cfg.getString("localDc"),
                     usedHostsPerRemoteDc: Int = Cassandra.cfg.getInt("usedHostsPerRemoteDc"),
                     allowRemoteDCsForLocalConsistencyLevel: Boolean = Cassandra.cfg.getBoolean("allowRemoteDCsForLocalConsistencyLevel")) extends BackendProfile {
  def `type` = CassandraType.name

  def getInstance = BaseCassandraInstance(
    hosts.split(","),
    user,
    password,
    replicationStrategy,
    replicationFactor,
    localDc,
    usedHostsPerRemoteDc,
    allowRemoteDCsForLocalConsistencyLevel
  )
}
case class AccumuloProfile(name: String, instance: String, zookeepers: String, user: String, password: String, strategy: Option[String] = None) extends BackendProfile {
  def `type` = AccumuloType.name
  def token = new PasswordToken(password)

  def getInstance = AccumuloInstance(instance, zookeepers, user, token)
}
case class HBaseProfile(name: String, master: String, zookeepers: String) extends BackendProfile {
  def `type` = HBaseType.name

  def getInstance = HBaseInstance(zookeepers.split(","), master)
}

case class BackendProfiles(backendProfiles: BackendProfile*)
