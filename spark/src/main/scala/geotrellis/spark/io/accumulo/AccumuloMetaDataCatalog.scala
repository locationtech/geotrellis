package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.accumulo.core.client.Connector
import org.apache.accumulo.core.data.{Key, Value, Mutation}
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.io.Text
import org.apache.spark.Logging

import scala.util.Try

class AccumuloMetaDataCatalog(connector: Connector, val catalogTable: String) extends Logging {
  {//create the metadata table if it does not exist
    val ops = connector.tableOperations()
    if (! ops.exists(catalogTable))
      ops.create(catalogTable)
  }

  private var metadata: Map[LayerId, (String, LayerMetaData)] = fetchAll

  def save(table: String, layerId: LayerId, metaData: LayerMetaData): Try[Unit] =
    Try {
      connector.write(catalogTable, AccumuloMetaDataCatalog.encodeMetaData(table, layerId, metaData))
      metadata = metadata updated (layerId, table -> metaData)
    }

  def get(layerId: LayerId): Try[(String, LayerMetaData)] =
    metadata
      .get(layerId)
      .toTry(new LayerNotFoundError(layerId.name, layerId.zoom))

  def fetchAll: Map[LayerId, (String, LayerMetaData)] = {
    val scan = connector.createScanner(catalogTable, new Authorizations())

    scan.map { case (key, value) =>
      val meta: LayerMetaData = AccumuloMetaDataCatalog.decodeMetaData(key, value)
      val table = key.getRow.toString
      val name: String = key.getColumnFamily.toString
      val layerId = LayerId(name, meta.level.id)
      layerId -> (table, meta)
    }.toMap
  }
}

object AccumuloMetaDataCatalog {
  import spray.json._
  import geotrellis.spark.json._

  def encodeMetaData(table: String, layerId: LayerId, md: LayerMetaData): Mutation = {
    val mutation = new Mutation(new Text(table))
    mutation.put(
      new Text(layerId.name), new Text(layerId.zoom.toString),
      System.currentTimeMillis(),
      new Value(md.toJson.prettyPrint.getBytes))
    mutation
  }

  def decodeMetaData(key: Key, value: Value): LayerMetaData = {
    new String(value.get().map(_.toChar)).parseJson.convertTo[LayerMetaData]
  }
}
