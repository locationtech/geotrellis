package geotrellis.spark.io.accumulo

import geotrellis.spark._
import org.apache.accumulo.core.client.BatchWriterConfig
import org.apache.accumulo.core.client.mapreduce.{ AccumuloOutputFormat, AccumuloInputFormat, InputFormatBase }
import org.apache.accumulo.core.data.{ Value, Key, Mutation }
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import scala.collection.mutable
import scala.util.{ Failure, Try }
import scala.collection.JavaConversions._
import scala.reflect.ClassTag

class TableNotFoundError(table: String) extends Exception(s"Target Accumulo table `$table` does not exist.")

trait AccumuloDriver[K] {
  def encode(layerId: LayerId, raster: RasterRDD[K]): RDD[(Text, Mutation)]
  def decode(rdd: RDD[(Key, Value)], metaData: RasterMetaData): RasterRDD[K]
  def setFilters(job: Job, layerId: LayerId, filters: FilterSet[K])
  def rowId(id: LayerId, key: K): String
  def getSplits(id: LayerId, rdd: RasterRDD[K], num: Int = 24): Seq[String]
  
  def load(sc: SparkContext, accumulo: AccumuloInstance)(id: LayerId, metaData: RasterMetaData, table: String, filters: FilterSet[K]): Try[RasterRDD[K]] =
    Try {
      val job = Job.getInstance(sc.hadoopConfiguration)
      accumulo.setAccumuloConfig(job)
      InputFormatBase.setInputTableName(job, table)
      setFilters(job, id, filters)
      val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value])
      decode(rdd, metaData)
    }

  /** NOTE: Accumulo will always perform destructive update, clobber param is not followed */
  def save(sc: SparkContext, accumulo: AccumuloInstance)(layerId: LayerId, raster: RasterRDD[K], table: String, clobber: Boolean): Try[Unit] =
    Try {
      // Create table if it doesn't exist.
      if (!accumulo.connector.tableOperations().exists(table))
        accumulo.connector.tableOperations().create(table)

      val ops = accumulo.connector.tableOperations()
      val groups = ops.getLocalityGroups(table)
      val newGroup: java.util.Set[Text] = Set(new Text(layerId.name))
      ops.setLocalityGroups(table, groups.updated(table, newGroup))
             

      val splits = getSplits(layerId, raster)
      accumulo.connector.tableOperations().addSplits(table, new java.util.TreeSet(splits.map(new Text(_))))

      val job = Job.getInstance(sc.hadoopConfiguration)
      accumulo.setAccumuloConfig(job)
      AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
      AccumuloOutputFormat.setDefaultTableName(job, table)
      encode(layerId, raster)
        .saveAsNewAPIHadoopFile(accumulo.instanceName, classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat], job.getConfiguration)
    }
}