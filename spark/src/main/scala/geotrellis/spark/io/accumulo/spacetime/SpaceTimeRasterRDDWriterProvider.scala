package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.index.zcurve._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat
import org.apache.accumulo.core.client.BatchWriterConfig

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.collection.mutable

import spire.syntax.cfor._

import org.joda.time.{DateTimeZone, DateTime}
import scala.collection.JavaConversions._

object SpaceTimeRasterRDDWriterProvider extends RasterRDDWriterProvider[SpaceTimeKey] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] =
    ZSpaceTimeKeyIndex.byYear

  /** TODO: What is the rules about the "num" parameter? */
  def getSplits(layerId: LayerId, metaData: RasterMetaData, keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey], num: Int = 48): List[String] = {
    val minIndex = index.toIndex(keyBounds.minKey)
    val maxIndex = index.toIndex(keyBounds.maxKey)
    val splitSize = (maxIndex - minIndex) / num

    val splits = mutable.ListBuffer[String]()

    cfor(minIndex)(_ < maxIndex, _ + splitSize) { i =>
      splits += rowId(layerId, i + 1)
    }

    splits.toList
  }

  def encode(layerId: LayerId, raster: RasterRDD[SpaceTimeKey], index: KeyIndex[SpaceTimeKey]): RDD[(Text, Mutation)] =
    raster
      .map { case (key, tile) =>
        val value = KryoSerializer.serialize[(SpaceTimeKey, Array[Byte])]( (key, tile.toBytes) )
        val mutation = new Mutation(rowId(layerId, index.toIndex(key)))
        mutation.put(
          new Text(layerId.name),
          timeText(key),
          System.currentTimeMillis(),
          new Value(value)
        )
        (null, mutation)
      }

  def writer(instance: AccumuloInstance, layerMetaData: AccumuloLayerMetaData, keyBounds: KeyBounds[SpaceTimeKey], index: KeyIndex[SpaceTimeKey])(implicit sc: SparkContext): RasterRDDWriter[SpaceTimeKey] =
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
        

        val splits = getSplits(layerId, layerMetaData.rasterMetaData, keyBounds, index)
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
