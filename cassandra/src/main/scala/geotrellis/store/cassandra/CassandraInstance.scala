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

package geotrellis.store.cassandra

import geotrellis.store.cassandra.conf.CassandraConfig
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder

import scala.collection.JavaConverters._
import java.net.{InetSocketAddress, URI}

object CassandraInstance {
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

trait CassandraInstance extends java.io.Serializable {
  val cassandraConfig: CassandraConfig

  /** Session constructor */
  def getSession: () => CqlSession

  @transient lazy val session: CqlSession = getSession()

  def ensureKeyspaceExists(keyspace: String, session: CqlSession): Unit =
    session.execute(
      SchemaBuilder
        .createKeyspace(keyspace)
        .ifNotExists()
        .withReplicationOptions(cassandraConfig.replicationOptions.asJava)
        .build()
    )

  def dropKeyspace(keyspace: String, session: CqlSession): Unit =
    session.execute(s"drop keyspace if exists $keyspace;")

  /** Without session close, for a manual session control */
  def withSession[T](block: CqlSession => T): T = block(session)

  /** With session close */
  def withSessionDo[T](block: CqlSession => T): T = {
    val session = getSession()
    try block(session) finally session.closeAsync()
  }

  def closeAsync = session.closeAsync()
}

case class BaseCassandraInstance(
  getSession: () => CqlSession,
  cassandraConfig: CassandraConfig
) extends CassandraInstance

object BaseCassandraInstance {
  def apply(getSession: () => CqlSession): BaseCassandraInstance =
    BaseCassandraInstance(getSession, CassandraConfig)

  def apply(
    hosts: Seq[String],
    username: String,
    password: String,
    cassandraConfig: CassandraConfig
  ): BaseCassandraInstance = {
    def getSession: () => CqlSession = () => {
      val builder =
        CqlSession
          .builder()
          .addContactPoints(hosts.map(InetSocketAddress.createUnresolved(_, cassandraConfig.port)).asJava)

      val authedBuilder =
        if(username.nonEmpty && password.nonEmpty) builder.withAuthCredentials(username, password)
        else builder

      authedBuilder.build()
    }

    BaseCassandraInstance(getSession, cassandraConfig)
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
  implicit def instanceToSession[T <: CassandraInstance](instance: T): CqlSession = instance.session

  def withCassandraInstance[T <: CassandraInstance, K](instance: T)(block: T => K): K = block(instance)
  def withCassandraInstanceDo[T <: CassandraInstance, K](instance: T)(block: T => K): K = try block(instance) finally instance.closeAsync()

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
    try block(instance) finally instance.closeAsync()
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
