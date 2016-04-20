package geotrellis.spark.io.cassandra

import com.datastax.driver.core.Cluster

trait CassandraInstance extends Serializable {
  val hosts: Seq[String]

  val username: String
  val password: String

  // column family is a table name
  val keyspace: String

  // probably there is a more conviniet way to do it
  val replicationStrategy: String
  val replicationFactor: Int

  @transient lazy val cluster = {
    val builder = Cluster.builder()
    hosts.map(builder.addContactPoint)
    builder.build()
  }

  @transient lazy val session = cluster.newSession()

  def ensureKeySpaceExists: Unit =
    session.execute(s"create keyspace if not exists ${keyspace} with replication = {'class': '${replicationStrategy}', 'replication_factor': ${replicationFactor} }")

  def close = { session.close(); cluster.close() }
  def closeAsync = { session.closeAsync(); cluster.closeAsync() }
}

case class BaseCassandraInstance(
  hosts: Seq[String],
  keyspace: String,
  username: String = "",
  password: String = "",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1) extends CassandraInstance
