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

  def load[K: ClassTag: Ordering](layerId: LayerId, filters: FilterSet[K]): Try[RasterRDD[K]] =
    (for {
      driver <- getDriver[K];
      (metaData, table) <- metaDataCatalog.load(layerId)
    } yield {
      driver.load(sc, instance)(layerId, table, metaData, filters)
    }).flatten

  def save[K: ClassTag](layerId: LayerId, rdd: RasterRDD[K], table: String): Try[Unit] =
    for {
      driver <- getDriver[K]
      _ <- driver.save(sc, instance)(layerId, rdd, table)
      _ <- metaDataCatalog.save(LayerMetaData(layerId, rdd.metaData), table)
    } yield Unit // TODO : If this fails at metadata save. We will have orphan data in a table
}
