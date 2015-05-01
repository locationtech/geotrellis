package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._
import geotrellis.spark.io.hadoop._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.fs.Path

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat
import org.apache.accumulo.core.client.BatchWriterConfig
import org.apache.accumulo.core.conf.{AccumuloConfiguration, Property}

import scala.collection.JavaConversions._
import scala.collection.mutable

import spire.syntax.cfor._

sealed trait AccumuloWriteStrategy
case object HdfsWriteStrategy extends AccumuloWriteStrategy
case object SocketWriteStrategy extends AccumuloWriteStrategy

trait RasterRDDWriter[K] {
  
  /** Getting KeyBounds most efficiently requires concrete knowladge of K */
  def getKeyBounds(rdd: RasterRDD[K]): KeyBounds[K]

  def getSplits(
    layerId: LayerId,
    metaData: RasterMetaData,
    keyBounds: KeyBounds[K],
    kIndex: KeyIndex[K],
    num: Int = 48
  ): List[String]

  def encode(
    layerId: LayerId,
    raster: RasterRDD[K],
    kIndex: KeyIndex[K]
  ): RDD[(Key, Value)]

  def write(
    instance: AccumuloInstance,
    layerMetaData: AccumuloLayerMetaData,
    keyBounds: KeyBounds[K],
    kIndex: KeyIndex[K]
  )(layerId: LayerId,
    raster: RasterRDD[K],
    strategy: AccumuloWriteStrategy
  )(implicit sc: SparkContext): Unit = {
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

    val kvPairs = encode(layerId, raster, kIndex)
    strategy match {
      case HdfsWriteStrategy => {
        def accumuloIngestDir: Path = {
          val accumuloConf =
            AccumuloConfiguration.getTableConfiguration(instance.connector, tileTable)
          new Path(accumuloConf.get(Property.INSTANCE_DFS_DIR), "ingest")
        }
        val conf = job.getConfiguration
        val outPath = HdfsUtils.tmpPath(accumuloIngestDir, s"${layerId.name}-${layerId.zoom}", conf)
        val failuresPath = outPath.suffix("-failures")

        try {
          HdfsUtils.ensurePathExists(failuresPath, conf)

          kvPairs
            .saveAsNewAPIHadoopFile(
              outPath.toString,
              classOf[Key],
              classOf[Value],
              classOf[AccumuloOutputFormat],
              job.getConfiguration)

          ops.importDirectory(tileTable, outPath.toString, failuresPath.toString, true)
        }
      }
      case SocketWriteStrategy => {
        def kvToMutation(kvRDD: RDD[(Key, Value)]): RDD[(Text, Mutation)] =
          kvRDD.map( KryoClosure { case (key, value) =>
              val mutation = new Mutation(key.getRow)
              mutation.put(
                layerId.name,
                key.getColumnQualifier,
                System.currentTimeMillis(),
                value)
              (null, mutation)
            })

        AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
        AccumuloOutputFormat.setDefaultTableName(job, tileTable)
        kvToMutation(kvPairs)
          .saveAsNewAPIHadoopFile(
            instance.instanceName,
            classOf[Text],
            classOf[Mutation],
            classOf[AccumuloOutputFormat],
            job.getConfiguration)
      }
    }
  }
}
