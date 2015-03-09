package geotrellis.spark.io.cassandra

import java.nio.ByteBuffer;

import geotrellis.spark._
import geotrellis.raster._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

import com.datastax.driver.core.DataType.{text, blob}
import com.datastax.driver.core.schemabuilder.SchemaBuilder

class TableNotFoundError(table: String) extends Exception(s"Target Cassandra table '$table' does not exist.")

trait CassandraDriver[K] extends Serializable {
  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[K]): RDD[(String, ByteBuffer)]
  def decode(rdd: RDD[(String, ByteBuffer)], metaData: RasterMetaData): RasterRDD[K]
  def rowId(id: LayerId, key: K): String

  def load(sc: SparkContext, keyspace: String)(
    id: LayerId, metaData: RasterMetaData, table: String, filters: FilterSet[K]): RasterRDD[K] = {

    val rdd: CassandraRDD[(String, ByteBuffer)] = 
      sc.cassandraTable[(String, ByteBuffer)](keyspace, table).select("id", "tile")

    val filteredRDD = applyFilter(rdd, id, filters)
    decode(rdd, metaData)
  }

  def loadTile(connector: CassandraConnector)(id: LayerId, metaData: RasterMetaData, table: String, key: K): Tile

  def save(connector: CassandraConnector, keyspace: String)(
    id: LayerId, raster: RasterRDD[K], table: String, clobber: Boolean): Unit = {
   
    // If not exists create table
    val schema = SchemaBuilder.createTable(keyspace, table).ifNotExists()
      .addPartitionKey("id", text)
      .addColumn("tile", blob)

    connector.withSessionDo(_.execute(schema))

    raster
      .sortBy { case (key, _) => rowId(id, key) }
      .map { case (key, tile) => (rowId(id, key), ByteBuffer.wrap(tile.toBytes)) }
      .saveToCassandra(keyspace, table, SomeColumns("id", "tile"))
  }

}
