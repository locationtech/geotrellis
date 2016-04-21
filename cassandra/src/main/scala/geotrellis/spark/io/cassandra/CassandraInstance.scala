package geotrellis.spark.io.cassandra

import com.datastax.driver.core.{Cluster, Session}

trait CassandraInstance extends Serializable {
  val hosts: Seq[String]

  val username: String
  val password: String

  val keyspace: String

  // probably there is a more convenient way to do it
  val replicationStrategy: String
  val replicationFactor: Int

  /** Functions to get cluster / session for custom logic, where function wrapping can have an impact on speed */
  def getCluster = Cluster.builder().addContactPoints(hosts: _*).build()
  def getSession = getCluster.connect()

  def ensureKeySpaceExists(session: Session): Unit =
    session.execute(s"create keyspace if not exists ${keyspace} with replication = {'class': '${replicationStrategy}', 'replication_factor': ${replicationFactor} }")

  /** Without session close, for a custom session close */
  def withSession[T](block: Session => T): T = block(getSession)

  /** With session close */
  def withSessionDo[T](block: Session => T): T = {
    val session = getSession
    try block(session) finally {
      session.closeAsync()
      session.getCluster.closeAsync()
    }
  }
}

case class BaseCassandraInstance(
  hosts: Seq[String],
  keyspace: String,
  username: String = "",
  password: String = "",
  replicationStrategy: String = "SimpleStrategy",
  replicationFactor: Int = 1) extends CassandraInstance
