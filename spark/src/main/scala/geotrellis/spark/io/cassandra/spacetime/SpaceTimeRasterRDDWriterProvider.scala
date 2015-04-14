package geotrellis.spark.io.cassandra.spacetime

import java.nio.ByteBuffer

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

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
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] =
    ZSpaceTimeKeyIndex.byYear

  def writer(instance: CassandraInstance, layerMetaData: CassandraLayerMetaData, keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey])(implicit sc: SparkContext): RasterRDDWriter[SpaceTimeKey] =
    new RasterRDDWriter[SpaceTimeKey] {
      def write(layerId: LayerId, raster: RasterRDD[SpaceTimeKey]): Unit = {
        val tileTable = layerMetaData.tileTable

        // If not exists create table
        val schema = SchemaBuilder.createTable(instance.keyspace, tileTable).ifNotExists()
          .addPartitionKey("id", text)
          .addClusteringColumn("name", text)
          .addColumn("time", text)
          .addColumn("value", blob)
        
        instance.session.execute(schema)
        
        raster
          .map { case (key, tile) =>
             val value = KryoSerializer.serialize[(SpaceTimeKey, Array[Byte])]( (key, tile.toBytes) )
 
             (rowId(layerId, index.toIndex(key)), layerId.name, timeText(key), value) 
          }
          .saveToCassandra(instance.keyspace, tileTable, SomeColumns("id", "name", "text", "value"))
      }
    }
}
