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
import com.datastax.driver.core.querybuilder.QueryBuilder.set
import com.datastax.driver.core.schemabuilder.SchemaBuilder

import com.datastax.spark.connector.cql.CassandraConnector

import org.apache.spark.Logging

import DefaultJsonProtocol._

class CassandraMetaDataCatalog(connector: CassandraConnector, val keyspace: String, val catalogTable: String) extends MetaDataCatalog[String] with Logging {

  var catalog: Map[(LayerId, TableName), LayerMetaData] = fetchAll
  val eq = QueryBuilder.eq _

  // Create the catalog table if it doesn't exist
  {
    val schema = SchemaBuilder.createTable(keyspace, catalogTable).ifNotExists()
      .addPartitionKey("name", text)
      .addClusteringColumn("table", text)
      .addClusteringColumn("zoom", cint)
      .addColumn("keyClass", text)
      .addColumn("metadata", text)
      .addColumn("histogram", text)
    
    connector.withSessionDo(_.execute(schema))
  }

  type TableName = String

  def load(layerId: LayerId): (LayerMetaData, TableName) = {
    val candidates = catalog
      .filterKeys( key => key._1 == layerId)

    candidates.size match {
      case 0 =>
        throw new LayerNotFoundError(layerId)
      case 1 =>
        val (key, value) = candidates.toList.head
        (value, key._2)
      case _ =>
        throw new MultipleMatchError(layerId)
    }
  }

  def load(layerId: LayerId, table: TableName): LayerMetaData = {
    catalog.get(layerId -> table) match {
      case Some(md) => md
      case None =>
        throw new LayerNotFoundError(layerId)
    }
  }

  def save(layerId: LayerId, table: TableName,  metaData: LayerMetaData, clobber: Boolean): Unit = {
    if (catalog.contains(layerId -> table)) {
      if (!clobber) {
        throw new LayerExistsError(layerId)
      }
    }

    catalog = catalog updated ((layerId -> table), metaData)

    val update = QueryBuilder.update(keyspace, catalogTable)
      .`with`(set("metadata", metaData.rasterMetaData.toJson.compactPrint))
      .and   (set("histogram", metaData.histogram.toJson.compactPrint))
      .and   (set("keyClass", metaData.keyClass))
      .where (eq("name", layerId.name))
      .and   (eq("table", table.toString))

    connector.withSessionDo(_.execute(update))
  }

  def fetchAll: Map[(LayerId, TableName), LayerMetaData] = {
    var data: Map[(LayerId, TableName), Map[String, String]] =
      Map.empty.withDefaultValue(Map.empty)

    val queryAll = QueryBuilder.select.all.from(keyspace, catalogTable)
    val results = connector.withSessionDo(_.execute(queryAll))
    val iter = results.iterator

    while (iter.hasNext) {
      val row = iter.next
      val name      = row.getString("name")
      val table     = row.getString("table")
      val zoom: Int = row.getInt("zoom")
      val keyClass  = row.getString("keyClass")
      val metaData  = row.getString("metadata")
      val histogram = row.getString("histogram")

      val layerId = LayerId(name, zoom)      

      val k = layerId -> table
      data = data updated (k, data(k) updated ("keyClass", keyClass))
      data = data updated (k, data(k) updated ("metadata", metaData))
      data = data updated (k, data(k) updated ("histogram", histogram))
    }

    def readLayerMetaData(map: Map[String, String]): LayerMetaData =
      LayerMetaData(
        keyClass =  map("keyClass"),
        rasterMetaData = map("metadata").parseJson.convertTo[RasterMetaData],
        histogram = map.get("histogram").map(_.parseJson.convertTo[Histogram])
      )

    data map { case (key, fieldMap) => key -> readLayerMetaData(fieldMap)}
  }
}
