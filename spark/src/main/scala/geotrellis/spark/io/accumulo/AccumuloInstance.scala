package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.tiling._
import org.apache.accumulo.core.client._
import org.apache.accumulo.core.client.mapreduce.{InputFormatBase, AccumuloInputFormat, AccumuloOutputFormat}
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken
import org.apache.accumulo.core.client.mapreduce.{AbstractInputFormat => AIF, AccumuloOutputFormat => AOF}
import org.apache.accumulo.core.data.{Value, Key, Mutation}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import com.typesafe.config.{ConfigFactory,Config}
import org.apache.accumulo.core.client.ClientConfiguration


trait AccumuloInstance  extends Serializable {
  def connector: Connector
  def instanceName: String
  def setAccumuloConfig(job: Job): Unit
}

object AccumuloInstance {
  def apply(instanceName: String, zookeeper: String, user: String, token: AuthenticationToken): AccumuloInstance = {
    val tokenBytes = AuthenticationToken.AuthenticationTokenSerializer.serialize(token)
    BaseAccumuloInstance(instanceName, zookeeper, user, tokenBytes)
  }
}

case class BaseAccumuloInstance(
  instanceName: String, zookeeper: String,
  user: String, tokenBytes: Array[Byte]) extends AccumuloInstance
{
  @transient lazy val token = AuthenticationToken.AuthenticationTokenSerializer.deserialize("AuthenticationToken", tokenBytes)
  @transient lazy val instance: Instance = instanceName match {
    case "fake" => new MockInstance("fake") //in-memory only
    case _      => new ZooKeeperInstance(instanceName, zookeeper)
  }
  @transient lazy val connector: Connector = instance.getConnector(user, token)


  def setAccumuloConfig(job: Job): Unit = {
    val clientConfig = ClientConfiguration
      .loadDefault()
      .withZkHosts(zookeeper)
      .withInstance(instanceName)

    
    if (instanceName == "fake") {
      AIF.setMockInstance(job, instanceName)
      AOF.setMockInstance(job, instanceName)
    }
    else {
      AIF.setZooKeeperInstance(job, clientConfig)
      AOF.setZooKeeperInstance(job, clientConfig)
    }

    AIF.setConnectorInfo(job, user, token)
    AOF.setConnectorInfo(job, user, token)
  }
}