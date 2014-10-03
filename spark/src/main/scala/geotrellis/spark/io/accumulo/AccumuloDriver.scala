package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.Driver
import org.apache.accumulo.core.client.BatchWriterConfig
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, AccumuloInputFormat, InputFormatBase}
import org.apache.accumulo.core.data.{Value, Key, Mutation}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

trait AccumuloDriver[K] extends Driver[K]{
  /** Accumulo table name */
  type Params = String

  def encode(raster: RasterRDD[K], layer: String): RDD[(Text, Mutation)]
  def decode(rdd: RDD[(Key, Value)], metaData: LayerMetaData): RasterRDD[K]
  def setFilters(job: Job, layer: String, metaData: LayerMetaData, filters: Seq[KeyFilter])

  def load(sc: SparkContext, accumulo: AccumuloInstance)
          (layer: String, table: String, metaData: LayerMetaData, filters: KeyFilter*): Option[RasterRDD[K]] =
  {
    val job = Job.getInstance(sc.hadoopConfiguration)
    accumulo.setAccumuloConfig(job)
    InputFormatBase.setInputTableName(job, table)
    setFilters(job, layer, metaData, filters)
    val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value])
    Some(decode(rdd, metaData)) // TODO what are the fail conditions, when is it none?
  }

  def save(sc: SparkContext, accumulo: AccumuloInstance)(raster: RasterRDD[K], layer: String, table: String) = {
    import org.apache.spark.SparkContext._

    //create output table if it does not exist, I hope I have permissions
    val tableOps = accumulo.connector.tableOperations()
    if (! tableOps.exists(table)) tableOps.create(table)

    val job = Job.getInstance(sc.hadoopConfiguration)
    accumulo.setAccumuloConfig(job)
    AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
    AccumuloOutputFormat.setDefaultTableName(job, table)

    encode(raster, layer).saveAsNewAPIHadoopFile(accumulo.instanceName,
      classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat],
      job.getConfiguration)
  }
}