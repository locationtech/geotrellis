/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.cassandra

import com.datastax.driver.core.policies.{DCAwareRoundRobinPolicy, TokenAwarePolicy}
import com.datastax.driver.core.{Cluster, Session}
import com.typesafe.config.ConfigFactory
import java.net.URI

object CassandraInstance {
  def apply(uri: URI): CassandraInstance = {
    import geotrellis.util.UriUtils._

    val zookeeper = uri.getHost
    val port = if (uri.getPort < 0) 2181 else uri.getPort
    val (user, pass) = getUserInfo(uri)
    val keyspace = Option(uri.getPath.drop(1))
      .getOrElse(Cassandra.cfg.getString("keyspace"))
    val attributeTable = Option(uri.getFragment)
      .getOrElse(Cassandra.cfg.getString("catalog"))

    BaseCassandraInstance(
      List(zookeeper),
      user.getOrElse(""),
      pass.getOrElse(""))
  }
}

trait CassandraInstance extends Serializable {
  val hosts: Seq[String]

  val username: String
  val password: String

  val replicationStrategy: String
  val replicationFactor: Int

  val localDc: String
  val usedHostsPerRemoteDc: Int
  val allowRemoteDCsForLocalConsistencyLevel: Boolean

  /** Functions to get cluster / session for custom logic, where function wrapping can have an impact on speed */
  def getCluster = Cluster.builder().withLoadBalancingPolicy(getLoadBalancingPolicy).addContactPoints(hosts: _*).build()
  def getSession = getCluster.connect()

  @transient lazy val cluster = getCluster
  @transient lazy val session = cluster.connect()

  def ensureKeyspaceExists(keyspace: String, session: Session): Unit =
    session.execute(s"create keyspace if not exists ${keyspace} with replication = {'class': '${replicationStrategy}', 'replication_factor': ${replicationFactor} }")

  def dropKeyspace(keyspace: String, session: Session): Unit =
    session.execute(s"drop keyspace if exists $keyspace;")

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

  def getLoadBalancingPolicy = {
    val builder = DCAwareRoundRobinPolicy.builder()
    if(localDc.nonEmpty) builder.withLocalDc(localDc)
    if(usedHostsPerRemoteDc > 0) builder.withUsedHostsPerRemoteDc(0)
    if(allowRemoteDCsForLocalConsistencyLevel) builder.allowRemoteDCsForLocalConsistencyLevel()

    new TokenAwarePolicy(builder.build())
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
  replicationFactor: Int = Cassandra.cfg.getInt("replicationFactor"),
  localDc: String = Cassandra.cfg.getString("localDc"),
  usedHostsPerRemoteDc: Int = Cassandra.cfg.getInt("usedHostsPerRemoteDc"),
  allowRemoteDCsForLocalConsistencyLevel: Boolean = Cassandra.cfg.getBoolean("allowRemoteDCsForLocalConsistencyLevel")) extends CassandraInstance

object Cassandra {
  lazy val cfg = ConfigFactory.load().getConfig("geotrellis.cassandra")

  implicit def instanceToSession[T <: CassandraInstance](instance: T): Session = instance.session

  def withCassandraInstance[T <: CassandraInstance, K](instance: T)(block: T => K): K = block(instance)
  def withCassandraInstanceDo[T <: CassandraInstance, K](instance: T)(block: T => K): K = try block(instance) finally instance.closeAsync
  def withBaseCassandraInstance[K](hosts: Seq[String],
                                   username: String = "",
                                   password: String = "",
                                   replicationStrategy: String = Cassandra.cfg.getString("replicationStrategy"),
                                   replicationFactor: Int = Cassandra.cfg.getInt("replicationFactor"),
                                   localDc: String = Cassandra.cfg.getString("localDc"),
                                   usedHostsPerRemoteDc: Int = Cassandra.cfg.getInt("usedHostsPerRemoteDc"),
                                   allowRemoteDCsForLocalConsistencyLevel: Boolean = Cassandra.cfg.getBoolean("allowRemoteDCsForLocalConsistencyLevel"))(block: BaseCassandraInstance => K): K =
    block(BaseCassandraInstance(hosts, username, password, replicationStrategy, replicationFactor, localDc, usedHostsPerRemoteDc, allowRemoteDCsForLocalConsistencyLevel))
  def withBaseCassandraInstanceDo[K](hosts: Seq[String],
                                     username: String = "",
                                     password: String = "",
                                     replicationStrategy: String = Cassandra.cfg.getString("replicationStrategy"),
                                     replicationFactor: Int = Cassandra.cfg.getInt("replicationFactor"),
                                     localDc: String = Cassandra.cfg.getString("localDc"),
                                     usedHostsPerRemoteDc: Int = Cassandra.cfg.getInt("usedHostsPerRemoteDc"),
                                     allowRemoteDCsForLocalConsistencyLevel: Boolean = Cassandra.cfg.getBoolean("allowRemoteDCsForLocalConsistencyLevel"))(block: BaseCassandraInstance => K): K = {
    val instance = BaseCassandraInstance(hosts, username, password, replicationStrategy, replicationFactor, localDc, usedHostsPerRemoteDc, allowRemoteDCsForLocalConsistencyLevel)
    try block(instance) finally instance.closeAsync
  }
}
