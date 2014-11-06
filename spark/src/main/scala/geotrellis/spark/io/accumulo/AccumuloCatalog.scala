package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, val metaDataCatalog: AccumuloMetaDataCatalog, val catalogConfig: AccumuloMetaDataCatalog.Config) 
    extends Catalog {
  type Params = String
  type SupportedKey[K] = AccumuloDriver[K]

  def paramsFor[K: ClassTag](layerId: LayerId): String = 
    catalogConfig.tableNameFor(layerId, implicitly[ClassTag[K]])

  def load[K: AccumuloDriver: ClassTag](metaData: LayerMetaData, table: String, filters: FilterSet[K]): Try[RasterRDD[K]] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.load(sc, instance)(metaData, table, filters)
  }

  def save[K: AccumuloDriver: ClassTag](rdd: RasterRDD[K], layerMetaData: LayerMetaData, table: String, clobber: Boolean): Try[Unit] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.save(sc, instance)(layerMetaData.id, rdd, table, clobber)
  }
}

object AccumuloCatalog {
  case class Config(
    /** This determines what table layer data from a specific layer and key should be saved in. */
    tableNameFor: (LayerId, ClassTag[_]) => String
  )

  object Config {
    val DEFAULT = 
      Config(
        tableNameFor = { (layerId: LayerId, classTag: ClassTag[_]) => 
          sys.error(s"Could not save layer $layerId. Default table mapping not provided, which means you must specify a table name when saving a layer.")
        }
      )
  }

  def apply(
    sc: SparkContext,
    instance: AccumuloInstance,
    metaDataCatalog: AccumuloMetaDataCatalog,
    catalogConfig: AccumuloMetaDataCatalog.Config = Config.DEFAULT): AccumuloCatalog =
    new AccumuloCatalog(sc, instance, metaDataCatalog, catalogConfig)
}
