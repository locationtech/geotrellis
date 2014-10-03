package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.Catalog
import org.apache.spark.SparkContext
import scala.reflect._

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, metaDataCatalog: MetaDataCatalog) extends Catalog {
  //type Source = AccumuloRddSource
  type DriverType[K] = AccumuloDriver[K]

  var loaders: Map[ClassTag[_],  AccumuloDriver[_]] = Map.empty
  def getLoader[K:ClassTag] = loaders.get(classTag[K]).map(_.asInstanceOf[AccumuloDriver[K]])

  def register[K: ClassTag](loader: AccumuloDriver[K]): Unit = loaders += classTag[K] -> loader

  def load[K:ClassTag](layerName: String, zoom: Int, filters: KeyFilter*): Option[RasterRDD[K]] =
  {
    for {
      (table, metaData) <- metaDataCatalog.get(Layer(layerName, zoom))
      loader <- getLoader[K]
    } yield {
      loader.load(sc, instance)(layerName, table, metaData, filters: _*) // TODO where did the filters go?
    }
  }.flatten

  def save[K: ClassTag](rdd: RasterRDD[K], layer: String, table: String): Unit = {
    metaDataCatalog.save(table, Layer(layer, rdd.metaData.level.id), rdd.metaData)

    for (loader <- getLoader[K]) {
      loader.save(sc, instance)(rdd, layer, table)
    }
  }
}