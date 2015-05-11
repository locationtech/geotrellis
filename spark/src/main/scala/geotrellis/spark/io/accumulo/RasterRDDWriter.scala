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
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, AccumuloFileOutputFormat}
import org.apache.accumulo.core.client.BatchWriterConfig
import org.apache.accumulo.core.conf.{AccumuloConfiguration, Property}

import scala.collection.JavaConversions._
import scala.collection.mutable

import scalaz.concurrent.Strategy
import scalaz.concurrent.Task
import scala.concurrent._
import scalaz.stream.async._
import scalaz.stream._

import spire.syntax.cfor._

sealed trait AccumuloWriteStrategy
case class HdfsWriteStrategy(ingestPath: Path) extends AccumuloWriteStrategy
case object SocketWriteStrategy extends AccumuloWriteStrategy

trait RasterRDDWriter[K] {
  def rowId(id: LayerId, index: Long): String  
  
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

    val ops = instance.connector.tableOperations()
    if (! ops.exists(tileTable)) 
      ops.create(tileTable)

    val groups = ops.getLocalityGroups(tileTable)
    val newGroup: java.util.Set[Text] = Set(new Text(layerId.name))
    ops.setLocalityGroups(tileTable, groups.updated(tileTable, newGroup))

    val job = Job.getInstance(sc.hadoopConfiguration)
    instance.setAccumuloConfig(job)

    val kvPairs = encode(layerId, raster, kIndex)
    strategy match {
      case HdfsWriteStrategy(ingestPath) => {
        val conf = job.getConfiguration
        val outPath = HdfsUtils.tmpPath(ingestPath, s"${layerId.name}-${layerId.zoom}", conf)
        val failuresPath = outPath.suffix("-failures")

        try {
          HdfsUtils.ensurePathExists(failuresPath, conf)
          // TODO: Can I get away with sorting ONLY the partition
          kvPairs
            .sortBy{ case (key, _) => key.getRow.toString }
            .saveAsNewAPIHadoopFile(
              outPath.toString,
              classOf[Key],
              classOf[Value],
              classOf[AccumuloFileOutputFormat],
              job.getConfiguration)

          ops.importDirectory(tileTable, outPath.toString, failuresPath.toString, true)
        }
      }

      case SocketWriteStrategy => {      
        // splits are required for efficient BatchWriter ingest
        val tserverCount = instance.connector.instanceOperations.getTabletServers.size
        val splits = getSplits(layerId, keyBounds, kIndex, tserverCount)
        ops.addSplits(tileTable, new java.util.TreeSet(splits.map(new Text(_))))

        val bcCon = sc.broadcast(instance.connector)
        kvPairs.foreachPartition { partition =>
          val writer = bcCon.value.createBatchWriter(tileTable, new BatchWriterConfig().setMaxMemory(128*1024*1024).setMaxWriteThreads(24))

          val mutations: Process[Task, Mutation] = 
            Process.unfold(partition){ iter => 
              if (iter.hasNext) {
                val (key, value) = iter.next
                val mutation = new Mutation(key.getRow)
                mutation.put(layerId.name, key.getColumnQualifier, System.currentTimeMillis(), value)
                Some(mutation, iter)
              } else  {
                None
              }
            }

          val write: Mutation => Process[Task, Unit] = { mutation =>
            Process eval Task { 
              writer.addMutation(mutation)
            }
          }   
          
          val results = nondeterminism.njoin(maxOpen = 24, maxQueued = 8) { mutations map (write) }
          results.run.run
        }
      }
    }
  }

  def getSplits(id: LayerId, kb: KeyBounds[K], ki: KeyIndex[K], count: Int): Seq[String] = {  
    var stack = ki.indexRanges(kb).toList
    def len(r: (Long, Long)) = r._2 - r._1 + 1l
    val total = stack.foldLeft(0l){ (s,r) => s + len(r) }
    val binWidth = total / count
    
    def splitRange(range: (Long, Long), take: Long): ((Long, Long), (Long, Long)) = {
      assert(len(range) > take)
      assert(take > 0)
      (range._1, range._1 + take - 1) -> (range._1 + take, range._2)
    }

    val arr = Array.fill[String](count - 1)(null)
    var sum = 0l
    var i = 0

    while (i < count - 1) {
      val nextStep = sum + len(stack.head)
      if (nextStep < binWidth){      
        sum += len(stack.head)
        stack = stack.tail
      } else if (nextStep == binWidth) {
        arr(i) = rowId(id, stack.head._2)
        stack = stack.tail
        i += 1
        sum = 0l
      } else {
        val (take, left) = splitRange(stack.head, binWidth - sum)
        stack = left :: stack.tail
        arr(i) = rowId(id, take._2)
        i += 1
        sum = 0l
      }
    }
    arr
  }
}
