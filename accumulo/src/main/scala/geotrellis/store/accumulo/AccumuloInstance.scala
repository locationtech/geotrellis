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

package geotrellis.store.accumulo

import org.apache.accumulo.core.client._
import org.apache.accumulo.core.client.mapreduce.{AbstractInputFormat => AIF, AccumuloOutputFormat => AOF}
import org.apache.accumulo.core.client.security.tokens.{AuthenticationToken, KerberosToken, PasswordToken}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import scala.jdk.CollectionConverters._
import java.net.URI

trait AccumuloInstance  extends Serializable {
  def client: AccumuloClient
  def instanceName: String
  def setAccumuloConfig(job: Job): Unit

  def ensureTableExists(tableName: String): Unit = {
    val ops = client.tableOperations()
    if (!ops.exists(tableName))
      ops.create(tableName)
  }

  def makeLocalityGroup(tableName: String, columnFamily: String): Unit = {
    val ops = client.tableOperations()
    val groups = ops.getLocalityGroups(tableName).asScala
    val newGroup: java.util.Set[Text] = Set(new Text(columnFamily)).asJava
    ops.setLocalityGroups(tableName, groups.clone().addOne((tableName, newGroup)).asJava)
  }
}

object AccumuloInstance {
  def apply(instanceName: String, zookeeper: String, user: String, token: AuthenticationToken): AccumuloInstance = {
    val tokenBytes = AuthenticationToken.AuthenticationTokenSerializer.serialize(token)
    val tokenClass = token.getClass.getCanonicalName
    BaseAccumuloInstance(instanceName, zookeeper, user, (tokenClass, tokenBytes))
  }

  def apply(uri: URI): AccumuloInstance = {
    import geotrellis.util.UriUtils._

    val zookeeper = if (uri.getPort != -1) s"${uri.getHost}:${uri.getPort}" else uri.getHost
    val instance = uri.getPath.drop(1)
    val (user, pass) = getUserInfo(uri)
    val useKerberos =
      ClientConfiguration
        .loadDefault()
        .get(ClientConfiguration.ClientProperty.INSTANCE_RPC_SASL_ENABLED)
        .toBoolean

    val (username:String,token:AuthenticationToken) = {
      if (useKerberos) {
        val token = new KerberosToken()
        (user.getOrElse(token.getPrincipal), token)
      } else (user.getOrElse("root"), new PasswordToken(pass.getOrElse("")))
    }
    AccumuloInstance(
      instance, zookeeper,
      username,
      token
    )
  }
}

case class BaseAccumuloInstance(
  instanceName: String, zookeeper: String,
  user: String, tokenBytes: (String, Array[Byte])) extends AccumuloInstance
{
  @transient lazy val token = AuthenticationToken.AuthenticationTokenSerializer.deserialize(tokenBytes._1, tokenBytes._2)
  @transient lazy val client: AccumuloClient =
    Accumulo.newClient().to(instanceName, zookeeper).as(user, token).build()

  def setAccumuloConfig(job: Job): Unit = {
    val clientConfig =
      ClientConfiguration
        .loadDefault()
        .withZkHosts(zookeeper)
        .withInstance(instanceName)

    AIF.setZooKeeperInstance(job, clientConfig)
    AOF.setZooKeeperInstance(job, clientConfig)

    AIF.setConnectorInfo(job, user, token)
    AOF.setConnectorInfo(job, user, token)
  }
}
