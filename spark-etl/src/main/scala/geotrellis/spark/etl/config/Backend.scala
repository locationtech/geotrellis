package geotrellis.spark.etl.config

import geotrellis.spark.io.cassandra.{Cassandra => ICassandra}
import org.apache.accumulo.core.client.security.tokens.PasswordToken

sealed trait Backend { val name: String }
case class Hadoop(name: String) extends Backend
case class S3(name: String, partitionsCount: Option[Int] = None) extends Backend
case class Cassandra(name: String, hosts: String, user: String, password: String,
                     replicationStrategy: String = ICassandra.cfg.getString("replicationStrategy"),
                     replicationFactor: Int = ICassandra.cfg.getInt("replicationFactor"),
                     localDc: String = ICassandra.cfg.getString("localDc"),
                     usedHostsPerRemoteDc: Int = ICassandra.cfg.getInt("usedHostsPerRemoteDc"),
                     allowRemoteDCsForLocalConsistencyLevel: Boolean = ICassandra.cfg.getBoolean("allowRemoteDCsForLocalConsistencyLevel")) extends Backend
case class Accumulo(name: String, instance: String, zookeepers: String, user: String, password: String, strategy: Option[String] = None) extends Backend {
  def token = new PasswordToken(password)
}
