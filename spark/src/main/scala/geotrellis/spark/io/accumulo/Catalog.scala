package geotrellis.spark.io.accumulo

import geotrellis.spark.rdd._
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.spark.SparkContext
import org.apache.accumulo.core.util.{Pair => JPair}

trait Catalog {
  /**
   * @param layer   Layer required, to know which zoom level to load
   */
  def load[K](layer: Layer): Option[RasterRDD[K]]

  /**
   * @param rdd       TmsRasterRDD needing saving
   * @param layerName Name for the layer to be saved, zoom information will be extracted from MetaData
   * @param prefix    Prefix to disambiguate the layers mapping to physical storage
   *                    ex: Accumulo Table Name, Hadoop Path prefix
   */
  def save[K](rdd: RasterRDD[K], layerName: String, prefix: String)
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
  //extends Catalog {
{
  /* I need zoom level right here in order to know which metadata to get */
  def load[K](layer: String, zoom: Int, filters: AccumuloFilter*)
             (implicit format: AccumuloFormat[K]): Option[RasterRDD[K]] =
  {
    metaDataCatalog.get(Layer(layer, zoom))
      .map { case (table, md) => instance.loadRaster[K](md, table, layer, filters: _*)(sc, format) }
  }

  def save[K](raster: RasterRDD[K], layerName: String, table: String)
             (implicit format: AccumuloFormat[K]): Unit =
  {
    //save metadata
    metaDataCatalog.save(table, Layer(layerName, raster.metaData.level.id), raster.metaData)

    //save tiles
    instance.saveRaster(raster, table, layerName)(sc, format)
  }
}