package geotrellis.spark.io.cassandra

import com.datastax.driver.core.{Cluster, Session}

trait CassandraInstance extends Serializable {
  val hosts: Seq[String]

  val username: String
  val password: String

  // column family is a table name
  val keyspace: String

  // probably there is a more conviniet way to do it
  val replicationStrategy: String
  val replicationFactor: Int

  def getCluster = Cluster.builder().addContactPoints(hosts: _*).build()
  def getSession = getCluster.connect()

  def ensureKeySpaceExists(session: Session): Unit =
    session.execute(s"create keyspace if not exists ${keyspace} with replication = {'class': '${replicationStrategy}', 'replication_factor': ${replicationFactor} }")
}

case class BaseCassandraInstance(
  hosts: Seq[String],
  keyspace: String,
  username: String = "",
  password: String = "",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1) extends CassandraInstance
