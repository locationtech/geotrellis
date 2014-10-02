package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.rdd._
import org.apache.accumulo.core.client.mapreduce.{AccumuloOutputFormat, InputFormatBase, AccumuloInputFormat}
import org.apache.accumulo.core.client.{BatchWriterConfig, Connector}
import org.apache.accumulo.core.data.{Value, Key}
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.SparkContext
import org.apache.accumulo.core.util.{Pair => JPair}

import scala.reflect._
import scala.language.higherKinds

trait RddSource
trait HdfsRddSource extends RddSource
trait AccumuloRddSource extends RddSource

trait RddLoader[K, RasterRDDSource]

//saving requires params that are specific to each Source
trait Catalog {
  type SOURCE <: RddSource
  type LOADER[K] <: RddLoader[K, SOURCE]

  def register[K:ClassTag](format: LOADER[K]): Unit
  def load[K:ClassTag](layerName: String, zoom: Int): Option[RasterRDD[K]]
  //def save = ???
}

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, metaDataCatalog: MetaDataCatalog) extends Catalog {
  type SOURCE = AccumuloRddSource
  type LOADER[K] = AccumuloRddLoader[K]

  var loaders: Map[ClassTag[_],  AccumuloRddLoader[_]] = Map.empty

  def register[K: ClassTag](loader: LOADER[K]): Unit = loaders += classTag[K] -> loader

  def load[K: ClassTag](layerName: String, zoom: Int): Option[RasterRDD[K]] = {
    for {
      (table, metaData) <- metaDataCatalog.get(Layer(layerName, zoom))
      loader <- loaders.get(classTag[K]).map(_.asInstanceOf[AccumuloRddLoader[K]])
    } yield {
      loader.load(sc, instance)(layerName, "someTable", metaData) // TODO where did the filters go?
    }
  }.flatten //Option[Option[RasterRDD[K]]] => Option[RasterRDD[K]
}


/**
 * Catalog of layers in a single Accumulo table.
 * All the layers in the table must share one TileFormat,
 * as it describes how the tile index is encoded.
 */
class OldAndBustedAccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, metaDataCatalog: MetaDataCatalog)
  //extends Catalog {
{
  def save[K](raster: RasterRDD[K], layerName: String, table: String)
             (implicit format: AccumuloFormat[K]): Unit =
  {
    //save metadata
    metaDataCatalog.save(table, Layer(layerName, raster.metaData.level.id), raster.metaData)

    //save tiles
    instance.saveRaster(raster, table, layerName)(sc, format)
  }
}

object Main {
  def run(): Unit ={
    val sc: SparkContext = ???
    val instance: AccumuloInstance = ???
    val mdc: MetaDataCatalog = ???

    val acccumuloCatalog = new AccumuloCatalog(sc, instance, mdc)
    acccumuloCatalog.register(RasterAccumuloRddLoader)
    acccumuloCatalog.load[TileId]("bestRaster", 23)
  }
}