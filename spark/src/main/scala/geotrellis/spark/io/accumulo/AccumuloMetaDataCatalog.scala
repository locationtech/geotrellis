package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.accumulo.core.client.Connector
import org.apache.accumulo.core.data.{Key, Value, Mutation}
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.io.Text
import org.apache.spark.Logging

import scala.util.Try
import scala.collection.mutable

class AccumuloMetaDataCatalog(connector: Connector, val catalogTable: String) extends MetaDataCatalog[String] with Logging {
  //create the metadata table if it does not exist
  {
    val ops = connector.tableOperations()
    if (! ops.exists(catalogTable))
      ops.create(catalogTable)
  }

  private val idToMetaData: mutable.Map[LayerId, (LayerMetaData, String)] = 
    mutable.Map(fetchAll.toSeq: _*)

  def save(metaData: LayerMetaData, table: String, clobber: Boolean): Try[Unit] =
    Try {
      if(idToMetaData.contains(metaData.id)) {
        if(clobber) {
          ??? // TODO: Need to implement deleting metadata out of catalog
        } else {
          throw new LayerExistsError(metaData.id)
        }
      }

      connector.write(catalogTable, AccumuloMetaDataCatalog.encodeMetaData(table, metaData))
      idToMetaData(metaData.id) = (metaData, table)
    }

  def load(layerId: LayerId): Try[(LayerMetaData, String)] =
    idToMetaData
      .get(layerId)
      .toTry(new LayerNotFoundError(layerId))

  def fetchAll: Map[LayerId, (LayerMetaData, String)] = {
    val scan = connector.createScanner(catalogTable, new Authorizations())

    scan.map { case (key, value) =>
      val meta: LayerMetaData = AccumuloMetaDataCatalog.decodeMetaData(key, value)
      val table = key.getRow.toString
      val name: String = key.getColumnFamily.toString
      val layerId = LayerId(name, meta.id.zoom)
      layerId -> (meta, table)
    }.toMap
  }
}

object AccumuloMetaDataCatalog {
  import spray.json._
  import geotrellis.spark.json._

  def encodeMetaData(table: String, md: LayerMetaData): Mutation = {
    val mutation = new Mutation(new Text(table))
    mutation.put(
      new Text(md.id.name), new Text(md.id.zoom.toString),
      System.currentTimeMillis(),
      new Value(md.toJson.prettyPrint.getBytes)
    )
    mutation
  }

  def decodeMetaData(key: Key, value: Value): LayerMetaData = {
    new String(value.get().map(_.toChar)).parseJson.convertTo[LayerMetaData]
  }
}
