package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.rdd._
import geotrellis.spark.tiling.TileCoordScheme

import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.data.{Range => ARange, Key, Value, Mutation}
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.accumulo.core.util.{Pair => JPair}
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

import scala.reflect._

trait Catalog {
  /**
   * @param layer   Layer required, to know which zoom level to load
   * @param bounds  TileBounds and the scheme which can be used to transform them
   * @return
   */
  def load(layer: Layer, bounds: Option[(TileBounds, TileCoordScheme)]): Option[RasterRDD]

  /**
   * @param rdd       RasterRDD needing saving
   * @param layerName Name for the layer to be saved, zoom information will be extracted from MetaData
   * @param prefix    Prefix to disambiguate the layers mapping to physical storage
   *                    ex: Accumulo Table Name, Hadoop Path prefix
   */
  def save(rdd: RasterRDD, layerName: String, prefix: String)
  /* Note:
    Perhaps it would be nice to have some kind of URL structure to parse and type check the prefix correctly
    an accumulo url could be: accumulo://instanceName/tableName
    an HDFS url could be: hdfs://path/to/raster
 */
}

/**
 * Catalog of layers in a single Accumulo table.
 * All the layers in the table must share one TileFormat,
 * as it describes how the tile index is encoded.
 */
class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, metaDataCatalog: MetaDataCatalog)
  extends Catalog
{
  private val format = TmsTileFormat //to abstract out later?

  /* I need zoom level right here in order to know which metadata to get */
  override def load(layer: Layer, bounds: Option[(TileBounds, TileCoordScheme)] = None): Option[RasterRDD] =
    metaDataCatalog.get(layer).map { case (table, md) =>
      val job = Job.getInstance(sc.hadoopConfiguration)
      instance.setAccumuloConfig(job.getConfiguration)
      InputFormatBase.setInputTableName(job, table)
      InputFormatBase.setRanges(job, format.ranges(layer, md, bounds))

      val _format = sc.broadcast(format)
      val rdd = sc.newAPIHadoopRDD(
        job.getConfiguration, classOf[AccumuloInputFormat], classOf[Key], classOf[Value]
      ).map { case (key, value) => _format.value.read(key, value)}

      new RasterRDD(rdd, md)
    }

  override def save(rdd: RasterRDD, layerName: String, table: String) = {
    //we could have asked for the layer directly, but this avoids possible mismatch between that and metaData
    val layer = Layer(layerName, rdd.metaData.level.id)
    val _conn = sc.broadcast(instance.connector)
    val _format = sc.broadcast(format)
    val _table = sc.broadcast(table)
    val _layer = sc.broadcast(layer)

    //save metadata
    metaDataCatalog.save(table, layer, rdd.metaData)

    //save tiles
    /**
    sc.runJob(rdd, { partition: Iterator[TmsTile] =>
      val cfg = new BatchWriterConfig()
      val writer = _conn.value.createBatchWriter(_table.value, cfg)
      partition.foreach { row =>
        writer.addMutation(_format.value.write(_layer.value, row))
      }
      writer.close()
    })
    */
    val bulk: RDD[(Text, Mutation)] = rdd.map { row =>
      val table = null
      val mut: Mutation = _format.value.write(_layer.value, row)
      (table, mut)
    }
    val job = new Job(sc.hadoopConfiguration)
    AccumuloOutputFormat.setZooKeeperInstance(job, instance.instanceName, instance.zookeeper)
    AccumuloOutputFormat.setConnectorInfo(job, instance.user, instance.token)
    AccumuloOutputFormat.setBatchWriterOptions(job, new BatchWriterConfig())
    AccumuloOutputFormat.setDefaultTableName(job, table)

    bulk.saveAsNewAPIHadoopFile(instance.instanceName,
      classOf[Text], classOf[Mutation], classOf[AccumuloOutputFormat],
      job.getConfiguration)
  }
}