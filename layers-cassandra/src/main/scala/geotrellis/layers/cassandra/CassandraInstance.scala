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

package geotrellis.layers.cassandra

import geotrellis.layers.cassandra.conf.CassandraConfig

import com.datastax.driver.core.policies.{DCAwareRoundRobinPolicy, TokenAwarePolicy}
import com.datastax.driver.core.{Cluster, Session}

import java.net.URI

object CassandraInstance {
  @volatile private var bigIntegerRegistered: Boolean = false

  def apply(uri: URI): CassandraInstance = {
    import geotrellis.util.UriUtils._

    val zookeeper = uri.getHost
    val (user, pass) = getUserInfo(uri)

    BaseCassandraInstance(
      List(zookeeper),
      user.getOrElse(""),
      pass.getOrElse("")
    )
  }
}

trait CassandraInstance extends Serializable {
  val cassandraConfig: CassandraConfig

  /** Functions to get cluster / session for custom logic, where function wrapping can have an impact on speed */
  def getCluster: () => Cluster
  def getSession: Session = getCluster().connect()

  @transient lazy val cluster: Cluster = getCluster()
  @transient lazy val session: Session = cluster.connect()

  def registerBigInteger(): Unit = {
    if (!CassandraInstance.bigIntegerRegistered) {
      cluster
        .getConfiguration()
        .getCodecRegistry()
        .register(BigIntegerIffBigint.instance)
      CassandraInstance.bigIntegerRegistered = true
    }
  }

  def ensureKeyspaceExists(keyspace: String, session: Session): Unit =
    session.execute(
      s"create keyspace if not exists ${keyspace} with replication = {'class': '${cassandraConfig.replicationStrategy}', " +
      s"'replication_factor': ${cassandraConfig.replicationFactor} }"
    )

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

  def closeAsync = {
    session.closeAsync()
    session.getCluster.closeAsync()
  }
}

case class BaseCassandraInstance(
  getCluster: () => Cluster,
  cassandraConfig: CassandraConfig
) extends CassandraInstance

object BaseCassandraInstance {
  def apply(getCluster: () => Cluster): BaseCassandraInstance =
    BaseCassandraInstance(getCluster, CassandraConfig)

  def apply(
    hosts: Seq[String],
    username: String,
    password: String,
    cassandraConfig: CassandraConfig
  ): BaseCassandraInstance = {
    def getLoadBalancingPolicy = {
      val builder = DCAwareRoundRobinPolicy.builder()
      if(cassandraConfig.localDc.nonEmpty) builder.withLocalDc(cassandraConfig.localDc)
      if(cassandraConfig.usedHostsPerRemoteDc > 0) builder.withUsedHostsPerRemoteDc(cassandraConfig.usedHostsPerRemoteDc)
      if(cassandraConfig.allowRemoteDCsForLocalConsistencyLevel) builder.allowRemoteDCsForLocalConsistencyLevel()

      new TokenAwarePolicy(builder.build())
    }

    def getCluster = () => {
      val builder =
        Cluster
          .builder()
          .withLoadBalancingPolicy(getLoadBalancingPolicy)
          .addContactPoints(hosts: _*)
          .withPort(cassandraConfig.port)
          .withCredentials(username, password)

      val authedBuilder =
        if(username.nonEmpty && password.nonEmpty) builder.withCredentials(username, password)
        else builder

      authedBuilder.build()
    }


    BaseCassandraInstance(getCluster, cassandraConfig)
  }

  def apply(
    hosts: Seq[String],
    username: String,
    password: String
  ): BaseCassandraInstance = apply(hosts, username, password, CassandraConfig)

  def apply(
    hosts: Seq[String],
    cassandraConfig: CassandraConfig
  ): BaseCassandraInstance = apply(hosts, "", "", cassandraConfig)

  def apply(hosts: Seq[String]): BaseCassandraInstance = apply(hosts, CassandraConfig)
}

object Cassandra {
  implicit def instanceToSession[T <: CassandraInstance](instance: T): Session = instance.session

  def withCassandraInstance[T <: CassandraInstance, K](instance: T)(block: T => K): K = block(instance)
  def withCassandraInstanceDo[T <: CassandraInstance, K](instance: T)(block: T => K): K = try block(instance) finally instance.closeAsync

  def withBaseCassandraInstance[K](hosts: Seq[String],
                                   username: String,
                                   password: String,
                                   cassandraConfig: CassandraConfig)(block: CassandraInstance => K): K =
    block(BaseCassandraInstance(hosts, username, password, cassandraConfig))
  def withBaseCassandraInstance[K](hosts: Seq[String], cassandraConfig: CassandraConfig)(block: CassandraInstance => K): K =
    withBaseCassandraInstance(hosts, "", "", cassandraConfig)(block)
  def withBaseCassandraInstance[K](hosts: Seq[String],
                                   username: String,
                                   password: String)(block: CassandraInstance => K): K =
    withBaseCassandraInstance(hosts, username, password, CassandraConfig)(block)
  def withBaseCassandraInstance[K](hosts: Seq[String])(block: CassandraInstance => K): K =
    withBaseCassandraInstance(hosts, "", "", CassandraConfig)(block)

  def withBaseCassandraInstanceDo[K](hosts: Seq[String],
                                     username: String,
                                     password: String,
                                     cassandraConfig: CassandraConfig)(block: CassandraInstance => K): K = {
    val instance = BaseCassandraInstance(hosts, username, password, cassandraConfig)
    try block(instance) finally instance.closeAsync
  }
  def withBaseCassandraInstanceDo[K](hosts: Seq[String],
                                     username: String,
                                     password: String)(block: CassandraInstance => K): K =
    withBaseCassandraInstanceDo(hosts, username, password, CassandraConfig)(block)
  def withBaseCassandraInstanceDo[K](hosts: Seq[String])(block: CassandraInstance => K): K =
    withBaseCassandraInstanceDo(hosts, "", "", CassandraConfig)(block)
  def withBaseCassandraInstanceDo[K](hosts: Seq[String],
                                     cassandraConfig: CassandraConfig)(block: CassandraInstance => K): K = {
    withBaseCassandraInstanceDo(hosts, "", "", cassandraConfig)(block)
  }
}
