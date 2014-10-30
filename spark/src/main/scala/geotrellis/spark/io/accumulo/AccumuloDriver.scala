package geotrellis.spark.io.accumulo

import geotrellis.spark._
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

trait AccumuloDriver[K] {
  def encode(layerId: LayerId, raster: RasterRDD[K]): RDD[(Text, Mutation)]
  def decode(rdd: RDD[(Key, Value)], metaData: RasterMetaData): RasterRDD[K]
  def setFilters(job: Job, layerId: LayerId, filters: FilterSet[K])

  def load(sc: SparkContext, accumulo: AccumuloInstance)
          (metaData: LayerMetaData, table: String, filters: FilterSet[K]): Try[RasterRDD[K]] =
  Try {
    val job = Job.getInstance(sc.hadoopConfiguration)
    accumulo.setAccumuloConfig(job)
    InputFormatBase.setInputTableName(job, table)
    setFilters(job, metaData.id, filters)
    val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value])
    decode(rdd, metaData.rasterMetaData)
  }

  def save(sc: SparkContext, accumulo: AccumuloInstance)
          (layerId: LayerId, raster: RasterRDD[K], table: String, clobber: Boolean): Try[Unit] =
    Try {
      // Create table if it doesn't exist.
      if (! accumulo.connector.tableOperations().exists(table)) 
        accumulo.connector.tableOperations().create(table)

      // TODO: How do we delete the tiles if they exist?

      val job = Job.getInstance(sc.hadoopConfiguration)
      accumulo.setAccumuloConfig(job)
      AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
      AccumuloOutputFormat.setDefaultTableName(job, table)
      encode(layerId, raster).saveAsNewAPIHadoopFile(accumulo.instanceName, classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat], job.getConfiguration)
    }
}
