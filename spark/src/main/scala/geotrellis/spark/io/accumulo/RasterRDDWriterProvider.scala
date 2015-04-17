package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat
import org.apache.accumulo.core.client.BatchWriterConfig

import scala.collection.JavaConversions._
import scala.collection.mutable

import spire.syntax.cfor._


trait RasterRDDWriterProvider[K] {

  def rowId(id: LayerId, index: Long): String

  /** TODO: What is the rules about the "num" parameter? */
  def getSplits(layerId: LayerId, metaData: RasterMetaData, keyBounds: KeyBounds[K], kIndex: KeyIndex[K], num: Int = 48): List[String] = {
    val minIndex = kIndex.toIndex(keyBounds.minKey)
    val maxIndex = kIndex.toIndex(keyBounds.maxKey)
    val splitSize = (maxIndex - minIndex) / num

    val splits = mutable.ListBuffer[String]()

    cfor(minIndex)(_ < maxIndex, _ + splitSize) { i =>
      splits += rowId(layerId, i + 1)
    }

    splits.toList
  }

  def encode(layerId: LayerId, raster: RasterRDD[K], kIndex: KeyIndex[K]): RDD[(Text, Mutation)]

  def writer(instance: AccumuloInstance, layerMetaData: AccumuloLayerMetaData, keyBounds: KeyBounds[K], kIndex: KeyIndex[K])(implicit sc: SparkContext): RasterRDDWriter[K] = {
    new RasterRDDWriter[K] {
      def write(layerId: LayerId, raster: RasterRDD[K]): Unit = {
        // Create table if it doesn't exist.
        val tileTable = layerMetaData.tileTable
        if (!instance.connector.tableOperations().exists(tileTable))
          instance.connector.tableOperations().create(tileTable)

        val ops = instance.connector.tableOperations()
        val groups = ops.getLocalityGroups(tileTable)
        val newGroup: java.util.Set[Text] = Set(new Text(layerId.name))
        ops.setLocalityGroups(tileTable, groups.updated(tileTable, newGroup))

        val splits = getSplits(layerId, layerMetaData.rasterMetaData, keyBounds, kIndex)
        instance.connector.tableOperations().addSplits(tileTable, new java.util.TreeSet(splits.map(new Text(_))))

        val job = Job.getInstance(sc.hadoopConfiguration)
        instance.setAccumuloConfig(job)
        AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
        AccumuloOutputFormat.setDefaultTableName(job, tileTable)
        encode(layerId, raster, kIndex)
          .saveAsNewAPIHadoopFile(instance.instanceName, classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat], job.getConfiguration)
      }
    }
  }
}
