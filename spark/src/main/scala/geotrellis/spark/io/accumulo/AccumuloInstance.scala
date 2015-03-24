package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.DefaultParams
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

case class AccumuloInstance(
  instanceName: String, zookeeper: String,
  user: String, token: AuthenticationToken)
{
  val instance: Instance = instanceName match {
    case "fake" => new MockInstance("fake") //in-memory only
    case _      => new ZooKeeperInstance(instanceName, zookeeper)
  }
  val connector = instance.getConnector(user, token)

  /** The value is specified in reference.conf, applications can overwrite it in their application.conf */
  val catalogTable: String = {
    ConfigFactory.load().getString("geotrellis.accumulo.catalog")
  }

  val metaDataCatalog = new AccumuloMetaDataCatalog(connector, catalogTable)

  def catalog(config: DefaultParams[String])(implicit sc: SparkContext) =
    AccumuloCatalog(sc, this, metaDataCatalog, config)

  def catalog(implicit sc: SparkContext) =
    AccumuloCatalog(sc, this, metaDataCatalog, AccumuloCatalog.BaseParamsConfig)

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
