package geotrellis.spark.io.cassandra

import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.ConfigFactory

trait CassandraInstance extends Serializable {
  val hosts: Seq[String]

  val username: String
  val password: String

  // probably there is a more convenient way to do it
  val replicationStrategy: String
  val replicationFactor: Int

  /** Functions to get cluster / session for custom logic, where function wrapping can have an impact on speed */
  def getCluster = Cluster.builder().addContactPoints(hosts: _*).build()
  def getSession = getCluster.connect()

  @transient lazy val cluster = getCluster
  @transient lazy val session = cluster.connect()

  def ensureKeyspaceExists(keyspace: String, session: Session): Unit =
    session.execute(s"create keyspace if not exists ${keyspace} with replication = {'class': '${replicationStrategy}', 'replication_factor': ${replicationFactor} }")

  /** Without session close, for a custom session close */
  def withSession[T](block: Session => T): T = block(session)

  /** With session close */
  def withSessionDo[T](block: Session => T): T = {
    val session = getSession
    try block(session) finally {
      session.closeAsync()
      session.getCluster.closeAsync()
    }
  }

  def closeAsync = {
    session.closeAsync()
    session.getCluster.closeAsync()
  }
}

case class BaseCassandraInstance(
  hosts: Seq[String],
  username: String = "",
  password: String = "",
  replicationStrategy: String = Cassandra.cfg.getString("replicationStrategy"),
  replicationFactor: Int = Cassandra.cfg.getInt("replicationFactor")) extends CassandraInstance

object Cassandra {
  lazy val cfg = ConfigFactory.load().getConfig("geotrellis.cassandra")

  implicit def instanceToSession[T <: CassandraInstance](instance: T): Session = instance.session

  def withCassandraInstance[T <: CassandraInstance, K](instance: T)(block: T => K): K = block(instance)
  def withCassandraInstanceDo[T <: CassandraInstance, K](instance: T)(block: T => K): K = try block(instance) finally instance.closeAsync
  def withBaseCassandraInstance[K](hosts: Seq[String],
                                   username: String = "",
                                   password: String = "",
                                   replicationStrategy: String = cfg.getString("replicationStrategy"),
                                   replicationFactor: Int = cfg.getInt("replicationFactor"))(block: BaseCassandraInstance => K): K =
    block(BaseCassandraInstance(hosts, username, password, replicationStrategy, replicationFactor))
  def withBaseCassandraInstanceDo[K](hosts: Seq[String],
                                     username: String = "",
                                     password: String = "",
                                     replicationStrategy: String = cfg.getString("replicationStrategy"),
                                     replicationFactor: Int = cfg.getInt("replicationFactor"))(block: BaseCassandraInstance => K): K = {
    val instance = BaseCassandraInstance(hosts, username, password, replicationStrategy, replicationFactor)
    try block(instance) finally instance.closeAsync
  }
}