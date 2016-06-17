package geotrellis.spark.etl.config.backend

import org.apache.accumulo.core.client.security.tokens.PasswordToken

sealed trait Backend { val name: String }
case class Hadoop(name: String) extends Backend
case class S3(name: String) extends Backend
case class Cassandra(name: String, hosts: String, user: String, password: String, replicationStrategy: String, replicationFactor: Int, localDc: String, usedHostsPerRemoteDc: Int, allowRemoteDCsForLocalConsistencyLevel: Boolean) extends Backend
case class Accumulo(name: String, instance: String, zookeepers: String, user: String, password: String, strategy: String) extends Backend {
  def token = new PasswordToken(password)
}
