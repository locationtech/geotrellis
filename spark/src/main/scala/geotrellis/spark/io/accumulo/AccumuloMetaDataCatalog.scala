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
    if (!ops.exists(catalogTable))
      ops.create(catalogTable)
  }

  private val idToMetaData: mutable.Map[LayerId, (RasterMetaData, String)] =
    mutable.Map(fetchAll.toSeq: _*)

  def save(id: LayerId, table: String, metaData: RasterMetaData, clobber: Boolean): Try[Unit] =
    Try {
      if (idToMetaData.contains(id)) {
        // If we want to clobber, by default Accumulo will overwrite it.
        // If not, let the user know.
        if (!clobber) {
          throw new LayerExistsError(id)
        }
      }

      connector.write(catalogTable, AccumuloMetaDataCatalog.encodeMetaData(table, id, metaData))
      idToMetaData(id) = (metaData, table)
    }

  def load(layerId: LayerId): Try[(RasterMetaData, String)] =
    idToMetaData
      .get(layerId)
      .toTry(new LayerNotFoundError(layerId))

  // TODO there should be a way to treat this consistently
  def load(layerId: LayerId, params: String): Try[RasterMetaData] =
    load(layerId).map(_._1)

  def fetchAll: Map[LayerId, (RasterMetaData, String)] = {
    val scan = connector.createScanner(catalogTable, new Authorizations())

    scan.map { case (key, value) =>
      val meta: RasterMetaData = AccumuloMetaDataCatalog.decodeMetaData(key, value)
      val table = key.getRow.toString
      val name: String = key.getColumnFamily.toString
      val zoom: Int = key.getColumnQualifier.toString.toInt
      val layerId = LayerId(name, zoom)
      layerId ->(meta, table)
    }.toMap
  }
}


object AccumuloMetaDataCatalog {
  import spray.json._
  import geotrellis.spark.json._

  def encodeMetaData(table: String, id: LayerId,  md: RasterMetaData): Mutation = {
    val mutation = new Mutation(new Text(table))
    mutation.put(
      new Text(id.name), new Text(id.zoom.toString),
      System.currentTimeMillis(),
      new Value(md.toJson.prettyPrint.getBytes)
    )
    mutation
  }

  def decodeMetaData(key: Key, value: Value): RasterMetaData = {
    new String(value.get().map(_.toChar)).parseJson.convertTo[RasterMetaData]
  }
}
