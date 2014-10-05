package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, metaDataCatalog: MetaDataCatalog) extends Catalog {
  //type Source = AccumuloRddSource
  type DriverType[K] = AccumuloDriver[K]

  var drivers: Map[ClassTag[_],  AccumuloDriver[_]] = Map.empty
  def getDriver[K:ClassTag]: Try[AccumuloDriver[K]] = drivers.get(classTag[K]) match {
    case Some(driver) => Success(driver.asInstanceOf[AccumuloDriver[K]])
    case None         => Failure(new DriverNotFound[K])
  }

  def register[K: ClassTag](loader: AccumuloDriver[K]): Unit = drivers += classTag[K] -> loader

  def load[K:ClassTag](layerName: String, zoom: Int, filters: FilterSet[K] = new FilterSet[K]()): Try[RasterRDD[K]] =
  {
    for {
      driver <- getDriver[K]
      md <- metaDataCatalog.get(Layer(layerName, zoom))
    } yield {
      driver.load(sc, instance)(layerName, md._1, md._2, filters)
    }
  }.flatten

  def save[K: ClassTag](rdd: RasterRDD[K], layer: String, table: String): Try[Unit] = {
    for {
      driver <- getDriver[K]
      _ <- driver.save(sc, instance)(rdd, layer, table)
      _ <- metaDataCatalog.save(table, Layer(layer, rdd.metaData.level.id), rdd.metaData)
    } yield Unit // TODO : If this fails at metadata save. We will have orphan data in a table
  }
}