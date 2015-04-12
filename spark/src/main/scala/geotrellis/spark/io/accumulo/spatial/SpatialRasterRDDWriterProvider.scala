package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat
import org.apache.accumulo.core.client.BatchWriterConfig

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

object SpatialRasterRDDWriterProvider extends RasterRDDWriterProvider[SpatialKey] {
  import SpatialRasterRDDIndex._

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] =
    new RowMajorSpatialKeyIndex(tileLayout.layoutCols)

  def getSplits(id: LayerId, rdd: RasterRDD[SpatialKey], index: KeyIndex[SpatialKey], num: Int = 24): Seq[String] = {
    import org.apache.spark.SparkContext._

    rdd
      .map(KryoClosure { case (key, _) => rowId(id, index.toIndex(key)) -> null })
      .sortByKey(ascending = true, numPartitions = num)
      .map(_._1)
      .mapPartitions{ iter => iter.take(1) }
      .collect
  }

  def encode(layerId: LayerId, raster: RasterRDD[SpatialKey], index: KeyIndex[SpatialKey]): RDD[(Text, Mutation)] =
    raster.map { case (key, tile) =>
      val value = KryoSerializer.serialize[(SpatialKey, Array[Byte])](key, tile.toBytes)
      val mutation = new Mutation(rowId(layerId, index.toIndex(key)))
      mutation.put(
        new Text(layerId.name), new Text(),
        System.currentTimeMillis(),
        new Value(value)
      )
      
      (null, mutation)
    }

  def writer(instance: AccumuloInstance, layerMetaData: AccumuloLayerMetaData, keyBounds: KeyBounds[SpatialKey], index: KeyIndex[SpatialKey])(implicit sc: SparkContext): RasterRDDWriter[SpatialKey] =
    new RasterRDDWriter[SpatialKey] {
      def write(layerId: LayerId, raster: RasterRDD[SpatialKey]): Unit = {
        // Create table if it doesn't exist.
        val tileTable = layerMetaData.tileTable
        if (!instance.connector.tableOperations().exists(tileTable))
          instance.connector.tableOperations().create(tileTable)

        val ops = instance.connector.tableOperations()
        val groups = ops.getLocalityGroups(tileTable)
        val newGroup: java.util.Set[Text] = Set(new Text(layerId.name))
        ops.setLocalityGroups(tileTable, groups.updated(tileTable, newGroup))

        val splits = getSplits(layerId, raster, index)
        instance.connector.tableOperations().addSplits(tileTable, new java.util.TreeSet(splits.map(new Text(_))))

        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
        AccumuloOutputFormat.setDefaultTableName(job, tileTable)
        encode(layerId, raster, index)
          .saveAsNewAPIHadoopFile(instance.instanceName, classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat], job.getConfiguration)
      }
    }
}
