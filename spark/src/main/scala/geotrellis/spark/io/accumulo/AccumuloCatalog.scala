package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, val metaDataCatalog: AccumuloMetaDataCatalog) extends Catalog {
  type Params = String
  type SupportedKey[K] = AccumuloDriver[K]

  // TODO: Figure out how we're going to create talbe names for layers.
  // Maybe inject a strategy for this?
  def paramsFor(layerId: LayerId): String = layerId.name

  def load[K: AccumuloDriver: ClassTag](metaData: LayerMetaData, table: String, filters: FilterSet[K]): Try[RasterRDD[K]] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.load(sc, instance)(metaData, table, filters)
  }

  def save[K: AccumuloDriver: ClassTag](rdd: RasterRDD[K], layerMetaData: LayerMetaData, table: String): Try[Unit] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.save(sc, instance)(layerMetaData.id, rdd, table)
  }
}
