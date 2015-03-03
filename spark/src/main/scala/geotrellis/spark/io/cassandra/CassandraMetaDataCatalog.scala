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

  // Create the catalog table if it doesn't exist
  {
    val schema = SchemaBuilder.createTable(keyspace, catalogTable).ifNotExists()
      .addPartitionKey("id", text)
      .addClusteringColumn("zoom", cint)
      .addColumn("keyClass", text)
      .addColumn("metadata", text)
      .addColumn("histogram", text)
    
    connector.withSessionDo(_.execute(schema))
  }

  var catalog: Map[(LayerId, TableName), LayerMetaData] = fetchAll
  val eq = QueryBuilder.eq _

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
      .where (eq("id", s"${table.toString}__${layerId.name}"))
      .and   (eq("zoom", layerId.zoom))

    connector.withSessionDo(_.execute(update))
  }

  def fetchAll: Map[(LayerId, TableName), LayerMetaData] = {
    var data: Map[(LayerId, TableName), LayerMetaData] =
      Map.empty

    val queryAll = QueryBuilder.select.all.from(keyspace, catalogTable)
    val results = connector.withSessionDo(_.execute(queryAll))
    val iter = results.iterator

    while (iter.hasNext) {
      val row = iter.next
      val Array(table, name) = row.getString("id").split("__")
      val zoom: Int  = row.getInt("zoom")
      val keyClass   = row.getString("keyClass")
      val rasterData = row.getString("metadata")
      val histogram  = Option(row.getString("histogram")) 

      val layerId = LayerId(name, zoom)
      val metaData = LayerMetaData(
        keyClass = keyClass,
        rasterMetaData = rasterData.parseJson.convertTo[RasterMetaData],
        histogram = histogram.map(_.parseJson.convertTo[Histogram])
      )

      val key = layerId -> table
      data = data updated (key, metaData)
    }

    return data
  }
}
