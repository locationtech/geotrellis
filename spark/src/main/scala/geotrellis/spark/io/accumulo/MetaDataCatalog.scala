package geotrellis.spark.io.accumulo

import geotrellis.spark._
import org.apache.accumulo.core.client.Connector
import org.apache.accumulo.core.data.{Key, Value, Mutation}
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.io.Text
import org.apache.spark.Logging

trait GenericMetaDataCatalog {
  def getLayerMetaData(layer: String, zoom: Int): Option[LayerMetaData]
}

class MetaDataCatalog(connector: Connector, val catalogTable: String) extends Logging {
  {//create the metadata table if it does not ext
    val ops = connector.tableOperations()
    if (! ops.exists(catalogTable))
      ops.create(catalogTable)
  }

  var metadata: Map[Layer, (String, LayerMetaData)] = fetchAll

  def save(table: String, layer: Layer , metaData: LayerMetaData) = {
    connector.write(catalogTable, MetaDataCatalog.encodeMetaData(table, layer, metaData))
    metadata = metadata updated (layer, table -> metaData)
  }

  def get(layer: Layer): Option[(String, LayerMetaData)] =
    metadata.get(layer)

  def fetchAll: Map[Layer, (String, LayerMetaData)] = {
    val scan = connector.createScanner(catalogTable, new Authorizations())

    scan.map{ case (key, value) =>
      val meta: LayerMetaData = MetaDataCatalog.decodeMetaData(key, value)
      val table = key.getRow.toString
      val name: String = key.getColumnFamily.toString
      val layer = Layer(name, meta.level.id)
      layer -> (table, meta)
    }.toMap
  }
}

object MetaDataCatalog {
  import spray.json._
  import geotrellis.spark.json._

  def encodeMetaData(table: String, layer: Layer, md: LayerMetaData): Mutation = {
    val mutation = new Mutation(new Text(table))
    mutation.put(
      new Text(layer.name), new Text(layer.zoom.toString) ,
      System.currentTimeMillis(),
      new Value(md.toJson.prettyPrint.getBytes))
    mutation
  }

  def decodeMetaData(key: Key, value: Value): LayerMetaData = {
    new String(value.get().map(_.toChar)).parseJson.convertTo[LayerMetaData]
  }
}