package geotrellis.spark.io.cassandra.spacetime

import java.nio.ByteBuffer

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

import com.datastax.driver.core.DataType.{text, blob}
import com.datastax.driver.core.schemabuilder.SchemaBuilder

object SpaceTimeRasterRDDWriterProvider extends RasterRDDWriterProvider[SpaceTimeKey] {
  import SpaceTimeRasterRDDIndex._

  def writer(instance: CassandraInstance, layerMetaData: CassandraLayerMetaData)(implicit sc: SparkContext): RasterRDDWriter[SpaceTimeKey] =
    new RasterRDDWriter[SpaceTimeKey] {
      def write(layerId: LayerId, raster: RasterRDD[SpaceTimeKey]): Unit = {

        val tileTable = layerMetaData.tileTable

        // If not exists create table
        val schema = SchemaBuilder.createTable(instance.keyspace, tileTable).ifNotExists()
          .addPartitionKey("id", text)
          .addClusteringColumn("name", text)
          .addColumn("tile", blob)
        
        instance.session.execute(schema)
        
        raster
          .sortBy { case (key, _) => rowId(layerId, key) }
          .map { case (key, tile) => (rowId(layerId, key), layerId.name, ByteBuffer.wrap(tile.toBytes)) }
          .saveToCassandra(instance.keyspace, tileTable, SomeColumns("id", "name", "tile"))
      }
    }
}
