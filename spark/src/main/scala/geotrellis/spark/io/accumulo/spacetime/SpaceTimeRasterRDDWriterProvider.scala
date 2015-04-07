package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.index.zcurve._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat
import org.apache.accumulo.core.client.BatchWriterConfig

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.joda.time.{DateTimeZone, DateTime}
import scala.collection.JavaConversions._

object SpaceTimeRasterRDDWriterProvider extends RasterRDDWriterProvider[SpaceTimeKey] {
  import SpaceTimeRasterRDDIndex._

  def timeText(key: SpaceTimeKey): Text =
    new Text(key.temporalKey.time.withZone(DateTimeZone.UTC).toString)

  /** TODO: What is the rules about the "num" parameter? */
  def getSplits(id: LayerId, metaData: RasterMetaData, num: Int = 48): List[String] = {
    val bounds = metaData.mapTransform(metaData.extent)
    // TODO: This information needs to come from RMD, which probably needs to vary with K, ya?
    val yearMin = 2000
    val yearMax  = 2100
    val zMin = Z3(bounds.colMin, bounds.rowMin, yearMin)
    val zMax = Z3(bounds.colMax, bounds.rowMax, yearMax)
    val ranges: Seq[(Long, Long)] = Z3.zranges(zMin, zMax)
    val totalSize = (yearMax - yearMin + 1) * (bounds.colMax - bounds.colMin + 1) * (bounds.rowMax - bounds.rowMin + 1)
    val splitSize = totalSize / num
    // now I just need to separate the things into bins

    var total: Long = 0
    var split: Int = 1
    var splits: List[String] = Nil
    for ((min, max) <- ranges) {
      total += (max - min + 1)
      if (splitSize * split < total) {
        split += 1
        splits ::= rowId(id, Z3(max + 1)) // TODO: Is there fencepost here? Is split inclusive on the left or right?
      }
    }

    splits
  }

  def encode(layerId: LayerId, raster: RasterRDD[SpaceTimeKey]): RDD[(Text, Mutation)] =
    raster
      .map { case (key, tile) =>
        val mutation = new Mutation(rowId(layerId, key))
        mutation.put(new Text(layerId.name), timeText(key),
          System.currentTimeMillis(), new Value(tile.toBytes()))
        (null, mutation)
      }

  def writer(instance: AccumuloInstance, layerMetaData: AccumuloLayerMetaData)(implicit sc: SparkContext): RasterRDDWriter[SpaceTimeKey] =
    new RasterRDDWriter[SpaceTimeKey] {
      def write(layerId: LayerId, raster: RasterRDD[SpaceTimeKey]): Unit = {
        // Create table if it doesn't exist.
        val tileTable = layerMetaData.tileTable
        if (!instance.connector.tableOperations().exists(tileTable))
          instance.connector.tableOperations().create(tileTable)

        val ops = instance.connector.tableOperations()
        val groups = ops.getLocalityGroups(tileTable)
        val newGroup: java.util.Set[Text] = Set(new Text(layerId.name))
        ops.setLocalityGroups(tileTable, groups.updated(tileTable, newGroup))
        

        val splits = getSplits(layerId, layerMetaData.rasterMetaData)
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
