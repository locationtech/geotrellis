package geotrellis.spark.io.accumulo


import geotrellis.spark.rdd.{TmsRasterRDD, LayerMetaData, RasterRDD}
import geotrellis.spark.tiling.{TileCoordScheme, TilingScheme}
import geotrellis.spark.{TileBounds, TileId, TmsTile}
import org.apache.accumulo.core.client._
import org.apache.accumulo.core.client.mapreduce.{InputFormatBase, AccumuloInputFormat, AccumuloOutputFormat}
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken
import org.apache.accumulo.core.client.mapreduce.lib.util.{ConfiguratorBase => CB}
import org.apache.accumulo.core.data.{Value, Key, Mutation}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD


case class AccumuloInstance(
  instanceName: String, zookeeper: String,
  user: String, token: AuthenticationToken)
{
  val instance: Instance = instanceName match {
    case "fake" => new MockInstance("fake") //in-memory only
    case _      => new ZooKeeperInstance(instanceName, zookeeper)
  }
  val connector = instance.getConnector(user, token)

  //TODO: read the table from the configuration
  val metaDataCatalog = new MetaDataCatalog(connector, "metadata")

  def tileCatalog(implicit sc: SparkContext) =
    new AccumuloCatalog(sc, this, metaDataCatalog)

  def setAccumuloConfig(conf: Configuration): Unit = {
    if (instanceName == "fake") {
      CB.setMockInstance(classOf[AccumuloInputFormat], conf, instanceName)
      CB.setMockInstance(classOf[AccumuloOutputFormat], conf, instanceName)
    }
    else {
      CB.setZooKeeperInstance(classOf[AccumuloInputFormat],conf, instanceName, zookeeper)
      CB.setZooKeeperInstance(classOf[AccumuloOutputFormat],conf, instanceName, zookeeper)
    }

    CB.setConnectorInfo(classOf[AccumuloInputFormat], conf, user, token)
    CB.setConnectorInfo(classOf[AccumuloOutputFormat], conf, user, token)
  }

  def setAccumuloConfig(job: Job): Unit = setAccumuloConfig(job.getConfiguration)

  def setAccumuloConfig(sc: SparkContext): Unit = setAccumuloConfig(sc.hadoopConfiguration)


  def saveRaster[K](raster: RasterRDD[K], table: String, layer: String)
    (implicit sc: SparkContext, format: AccumuloFormat[K])
  {
    import org.apache.spark.SparkContext._

    //create output table if it does not exist
    val tableOps = connector.tableOperations()
    if (! tableOps.exists(table)) tableOps.create(table)

    val job = Job.getInstance(sc.hadoopConfiguration)
    setAccumuloConfig(job)
    AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
    AccumuloOutputFormat.setDefaultTableName(job, table)

    format.encode(raster, layer).saveAsNewAPIHadoopFile(instanceName,
      classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat],
      job.getConfiguration)
  }

  def loadRaster[K](metaData: LayerMetaData, table: String, layer: String, filters: AccumuloFilter*)
    (implicit sc: SparkContext, format: AccumuloFormat[K]) : RasterRDD[K] =
  {
    import org.apache.spark.SparkContext._

    val job = Job.getInstance(sc.hadoopConfiguration)
    setAccumuloConfig(job)
    InputFormatBase.setInputTableName(job, table)

    format.setFilters(job, metaData, filters)
    val rdd = sc.newAPIHadoopRDD(job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value])
    format.decode(rdd, metaData)
  }
}