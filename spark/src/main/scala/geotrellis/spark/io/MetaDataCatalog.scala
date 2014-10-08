package geotrellis.spark.io

import geotrellis.spark._

import org.apache.accumulo.core.client.Connector
import org.apache.accumulo.core.data.{Key, Value, Mutation}
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.io.Text
import org.apache.spark.Logging

import scala.util.Try

trait MetaDataCatalog {
  def getLayerMetaData(layer: String, zoom: Int): Option[LayerMetaData]
}
