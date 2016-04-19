package geotrellis.spark.io.cassandra

import com.datastax.driver.core.Cluster

trait CassandraInstance extends Serializable {
  val hosts: Seq[String]

  val username: String
  val password: String

  // column family is a table name
  val keySpace: String

  // probably there is a more conviniet way to do it
  val replicationStrategy: String
  val replicationFactor: Int

  @transient lazy val cluster = {
    val builder = Cluster.builder()
    hosts.map(builder.addContactPoint)
    builder.build()
  }

  @transient lazy val session = cluster.connect()

  def ensureKeySpaceExists: Unit =
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS ${keySpace} WITH REPLICATION = {'class': '${replicationStrategy}', 'replication_factor': ${replicationFactor} }")

  def close = { session.close(); cluster.close() }
  def closeAsync = { session.closeAsync(); cluster.closeAsync() }
}

case class BaseCassandraInstance(
  hosts: Seq[String],
  keySpace: String,
  username: String = "",
  password: String = "",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1) extends CassandraInstance
