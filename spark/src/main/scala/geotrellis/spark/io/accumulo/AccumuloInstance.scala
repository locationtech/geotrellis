package geotrellis.spark.io.accumulo


import geotrellis.spark.{TileId, TmsTile}
import org.apache.accumulo.core.client._
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat
import org.apache.accumulo.core.client.mapreduce.lib.util.ConfiguratorBase
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.lib.util.{ConfiguratorBase => CB}
import org.apache.spark.SparkContext
import org.apache.hadoop.conf.Configuration


case class AccumuloInstance(instanceName: String, zookeeper: String, user: String, token: AuthenticationToken) {
  def instance = new ZooKeeperInstance(instanceName, zookeeper)
  def connector = instance.getConnector(user, token)

  def initAccumuloInputFormat(sc: SparkContext): Unit =
    initAccumuloInputFormat(sc.hadoopConfiguration)

  def initAccumuloInputFormat(conf: Configuration): Unit = {
      CB.setZooKeeperInstance(classOf[AccumuloInputFormat],conf, instanceName, zookeeper)
      CB.setConnectorInfo(classOf[AccumuloInputFormat], conf, user, token)
  }
}
