package geotrellis.spark.io.accumulo

import geotrellis.spark.{TileId, TmsTile}
import org.apache.accumulo.core.client._
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken
import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}

case class AccumuloInstance(instanceName: String, zookeeper: String, user: String, token: AuthenticationToken) {
  val instance = new ZooKeeperInstance(instanceName, zookeeper)
  val connector = instance.getConnector(user, token)
}
