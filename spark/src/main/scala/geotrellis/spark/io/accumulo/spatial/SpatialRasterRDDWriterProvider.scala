package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._

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

  def getSplits(id: LayerId, rdd: RasterRDD[SpatialKey], num: Int = 24): Seq[String] = {
    import org.apache.spark.SparkContext._

    rdd
      .map { case (key, _) => rowId(id, key) -> null }
      .sortByKey(ascending = true, numPartitions = num)
      .map(_._1)
      .mapPartitions{ iter => iter.take(1) }
      .collect
  }

  def encode(layerId: LayerId, raster: RasterRDD[SpatialKey]): RDD[(Text, Mutation)] =
    raster.map { case (key, tile) =>
      val mutation = new Mutation(rowId(layerId, key))
      mutation.put(
        new Text(layerId.name), new Text(),
        System.currentTimeMillis(),
        new Value(tile.toBytes())
      )
      
      (null, mutation)
    }

  def writer(instance: AccumuloInstance, metaDataCatalog: Writer[LayerId, AccumuloLayerMetaData], tileTable: String)(implicit sc: SparkContext): RasterRDDWriter[SpatialKey] =
    new RasterRDDWriter[SpatialKey] {
      def write(layerId: LayerId, raster: RasterRDD[SpatialKey]): Unit = {
        // Create table if it doesn't exist.
        if (!instance.connector.tableOperations().exists(tileTable))
          instance.connector.tableOperations().create(tileTable)

        val ops = instance.connector.tableOperations()
        val groups = ops.getLocalityGroups(tileTable)
        val newGroup: java.util.Set[Text] = Set(new Text(layerId.name))
        ops.setLocalityGroups(tileTable, groups.updated(tileTable, newGroup))
        

        val splits = getSplits(layerId, raster)
        instance.connector.tableOperations().addSplits(tileTable, new java.util.TreeSet(splits.map(new Text(_))))

        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
        AccumuloOutputFormat.setDefaultTableName(job, tileTable)
        encode(layerId, raster)
          .saveAsNewAPIHadoopFile(instance.instanceName, classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat], job.getConfiguration)
      }
    }
}
