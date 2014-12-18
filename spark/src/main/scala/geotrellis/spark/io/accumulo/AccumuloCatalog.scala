package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}
import geotrellis.spark.op.stats._

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

  def load[K: AccumuloDriver: ClassTag](id: LayerId, metaData: RasterMetaData, table: String, filters: FilterSet[K]): RasterRDD[K] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.load(sc, instance)(id, metaData, table, filters)
  }

  def save[K: SupportedKey : ClassTag](id: LayerId, table: String, rdd: RasterRDD[K], clobber: Boolean): Unit = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.save(sc, instance)(id, rdd, table, clobber)

    val metaData = LayerMetaData(
      rasterMetaData = rdd.metaData,
      keyClass = classTag[K].toString,
      histogram = Some(rdd.histogram)
    )
    metaDataCatalog.save(id, table: Params, metaData, clobber)
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
