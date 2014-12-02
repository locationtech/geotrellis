package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, 
  val metaDataCatalog: AccumuloMetaDataCatalog, 
  val paramsConfig: DefaultParams[String]
) extends Catalog {
  type Params = String
  type SupportedKey[K] = AccumuloDriver[K]

  def paramsFor[K: SupportedKey: ClassTag](id: LayerId): String =
    paramsConfig.paramsFor[K](id) match {
      case Some(params) => params
      case None => sys.error(s"Default Params for '$id' not found.")
    }

  def load[K: AccumuloDriver: ClassTag](id: LayerId, metaData: RasterMetaData, table: String, filters: FilterSet[K]): Try[RasterRDD[K]] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.load(sc, instance)(id, metaData, table, filters)
  }

  def save[K: SupportedKey : ClassTag](id: LayerId, table: String, rdd: RasterRDD[K], clobber: Boolean): Try[Unit] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.save(sc, instance)(id, rdd, table, clobber)
    metaDataCatalog.save(id, table.asInstanceOf[Params], rdd.metaData, clobber)
  }
}

object AccumuloCatalog {

  /** Use val as base in Builder pattern to make your own table mappings. */
  val BaseParamsConfig = new DefaultParams[String](Map.empty, Map.empty)

  def apply(
    sc: SparkContext,
    instance: AccumuloInstance,
    metaDataCatalog: AccumuloMetaDataCatalog,
    paramsConfig: DefaultParams[String] = BaseParamsConfig): AccumuloCatalog =

    new AccumuloCatalog(sc, instance, metaDataCatalog, paramsConfig)
}