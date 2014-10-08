package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, metaDataCatalog: AccumuloMetaDataCatalog) extends Catalog {
  type DriverType[K] = AccumuloDriver[K]

  var drivers: Map[ClassTag[_],  AccumuloDriver[_]] = Map.empty
  def getDriver[K:ClassTag]: Try[AccumuloDriver[K]] = 
    drivers.get(classTag[K]) match {
      case Some(driver) => Success(driver.asInstanceOf[AccumuloDriver[K]])
      case None         => Failure(new DriverNotFoundError[K])
    }

  def register[K: ClassTag](loader: AccumuloDriver[K]): Unit = drivers += classTag[K] -> loader

  def load[K:ClassTag](layerId: LayerId, filters: FilterSet[K] = new FilterSet[K]()): Try[RasterRDD[K]] =
    (for {
      driver <- getDriver[K]
      md <- metaDataCatalog.get(layerId)
    } yield {
      driver.load(sc, instance)(layerId.name, md._1, md._2, filters)
    }).flatten

  def save[K: ClassTag](rdd: RasterRDD[K], layerName: String, table: String): Try[Unit] =
    for {
      driver <- getDriver[K]
      _ <- driver.save(sc, instance)(rdd, layerName, table)
      _ <- metaDataCatalog.save(table, LayerId(layerName, rdd.metaData.level.id), rdd.metaData)
    } yield Unit // TODO : If this fails at metadata save. We will have orphan data in a table
}
