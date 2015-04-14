package geotrellis.spark.io.cassandra

import geotrellis.raster.histogram.Histogram
import geotrellis.raster.io.json._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import spray.json._

import com.datastax.driver.core.DataType.text
import com.datastax.driver.core.DataType.cint
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{set, eq => eqs}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.Session

import org.apache.spark.Logging

import DefaultJsonProtocol._

import scala.collection.mutable

case class CassandraLayerMetaData(
  rasterMetaData: RasterMetaData,
  histogram: Option[Histogram],
  keyClass: String,
  tileTable: String
)

class CassandraLayerMetaDataCatalog(session: Session, val keyspace: String, val catalogTable: String) extends Store[LayerId, CassandraLayerMetaData] with Logging {

  // Create the catalog table if it doesn't exist
  {
    val schema = SchemaBuilder.createTable(keyspace, catalogTable).ifNotExists()
      .addPartitionKey("id", text)
      .addClusteringColumn("zoom", cint)
      .addColumn("keyClass", text)
      .addColumn("metadata", text)
      .addColumn("histogram", text)
    
    session.execute(schema)
  }

  var catalog: mutable.Map[LayerId, CassandraLayerMetaData] = fetchAll

  def zoomLevelsFor(layerName: String): Seq[Int] = {
    catalog.keys.filter(_.name == layerName).map(_.zoom).toSeq
  }

  type TableName = String

  def read(layerId: LayerId): CassandraLayerMetaData = {
    val candidates = catalog
      .filterKeys(_ == layerId)

    candidates.size match {
      case 0 =>
        throw new LayerNotFoundError(layerId)
      case 1 =>
        val (key, value) = candidates.toList.head
        value
      case _ =>
        throw new MultipleMatchError(layerId)
    }
  }

  def write(layerId: LayerId, metaData: CassandraLayerMetaData): Unit = {
    val tileTable = metaData.tileTable
    catalog(layerId) = metaData

    val update = QueryBuilder.update(keyspace, catalogTable)
      .`with`(set("metadata", metaData.rasterMetaData.toJson.compactPrint))
      .and   (set("histogram", metaData.histogram.toJson.compactPrint))
      .and   (set("keyClass", metaData.keyClass))
      .where (eqs("id", s"${tileTable}__${layerId.name}"))
      .and   (eqs("zoom", layerId.zoom))

    session.execute(update)
  }

  def fetchAll: mutable.Map[LayerId, CassandraLayerMetaData] = {
    var data: mutable.Map[LayerId, CassandraLayerMetaData] =
      mutable.Map.empty

    val queryAll = QueryBuilder.select.all.from(keyspace, catalogTable)
    val results = session.execute(queryAll)
    val iter = results.iterator

    while (iter.hasNext) {
      val row = iter.next
      val Array(tileTable, name) = row.getString("id").split("__")
      val zoom: Int  = row.getInt("zoom")
      val keyClass   = row.getString("keyClass")
      val rasterData = row.getString("metadata")
      val histogram  = row.getString("histogram") match {
        case "null" => None
        case hist: String => Some(hist)
      }

      val layerId = LayerId(name, zoom)
      val metaData = CassandraLayerMetaData(
        keyClass = keyClass,
        rasterMetaData = rasterData.parseJson.convertTo[RasterMetaData],
        histogram = histogram.map(_.parseJson.convertTo[Histogram]),
        tileTable = tileTable
      )

      data = data updated (layerId, metaData)
    }

    return data
  }
}
