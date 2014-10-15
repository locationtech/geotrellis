package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.Driver
import org.apache.accumulo.core.client.BatchWriterConfig
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, AccumuloInputFormat, InputFormatBase}
import org.apache.accumulo.core.data.{Value, Key, Mutation}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import scala.util.{Failure, Try}

class TableNotFoundError(table: String) extends Exception(s"Target Accumulo table `$table` does not exist.")

trait AccumuloDriver[K] extends Driver[K]{
  /** Accumulo table name */
  type Params = String

  def encode(layerId: LayerId, raster: RasterRDD[K]): RDD[(Text, Mutation)]
  def decode(rdd: RDD[(Key, Value)], metaData: RasterMetaData): RasterRDD[K]
  def setFilters(job: Job, layerId: LayerId, metaData: LayerMetaData, filters: Seq[KeyFilter])

  def load(sc: SparkContext, accumulo: AccumuloInstance)
          (layerId: LayerId, table: String, metaData: LayerMetaData, filters: FilterSet[K]): Try[RasterRDD[K]] =
  Try {
    val job = Job.getInstance(sc.hadoopConfiguration)
    accumulo.setAccumuloConfig(job)
    InputFormatBase.setInputTableName(job, table)
    setFilters(job, layerId, metaData, filters.filters)
    val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value])
    decode(rdd, metaData.rasterMetaData)
  }

  def save(sc: SparkContext, accumulo: AccumuloInstance)
          (layerId: LayerId, raster: RasterRDD[K], table: String): Try[Unit] = {
    if (! accumulo.connector.tableOperations().exists(table))
      Failure[Unit](new TableNotFoundError(table))
    else Try {
      val job = Job.getInstance(sc.hadoopConfiguration)
      accumulo.setAccumuloConfig(job)
      AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
      AccumuloOutputFormat.setDefaultTableName(job, table)
      encode(layerId, raster).saveAsNewAPIHadoopFile(accumulo.instanceName, classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat], job.getConfiguration)
    }
  }
}
