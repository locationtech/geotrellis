package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.json._
import geotrellis.raster.json._
import geotrellis.spark.io._
import geotrellis.raster._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}
import geotrellis.spark.op.stats._
import spray.json.DefaultJsonProtocol._
import AttributeCatalog._

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, 
  val attributes: AccumuloAttributeCatalog, 
  val paramsConfig: DefaultParams[String]
) extends Catalog {
  type Params = String
  type SupportedKey[K] = AccumuloDriver[K]
  
  def paramsFor[K: SupportedKey: ClassTag](id: LayerId): String =
    paramsConfig.paramsFor[K](id) match {
      case Some(params) => params
      case None => sys.error(s"Default Params for '$id' not found.")
    }

  def load[K: AccumuloDriver: ClassTag](id: LayerId, filters: FilterSet[K]): RasterRDD[K] = {
    val metaData = attributes.load[RasterMetaData](id, METADATA_FIELD)    
    val table = attributes.load[String](id, TABLENAME_FIELD)

    val driver = implicitly[AccumuloDriver[K]]
    driver.load(sc, instance)(id, metaData, table, filters)
  }

  def loadTile[K: AccumuloDriver: ClassTag](id: LayerId, key: K): Tile = {
    val metaData = attributes.load[RasterMetaData](id, METADATA_FIELD)
    val table = attributes.load[String](id, TABLENAME_FIELD)
    loadTile(id, metaData, table, key)
  }

  def loadTile[K: AccumuloDriver: ClassTag](id: LayerId, metaData: RasterMetaData, table: String, key: K): Tile = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.loadTile(instance.connector, table)(id, metaData, key)
  }

  def save[K: SupportedKey : ClassTag](id: LayerId, table: String, rdd: RasterRDD[K], clobber: Boolean): Unit = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.save(sc, instance)(id, rdd, table, clobber)
    
    attributes.save(id, METADATA_FIELD, rdd.metaData)
    attributes.save(id, TABLENAME_FIELD, table)
    attributes.save(id, HISTOGRAM_FIELD, rdd.histogram)
  }
}

object AccumuloCatalog {
  /** Use val as base in Builder pattern to make your own table mappings. */
  val BaseParamsConfig = new DefaultParams[String](Map.empty, Map.empty)

  def apply(
    sc: SparkContext,
    instance: AccumuloInstance,
    attributes: AccumuloAttributeCatalog,
    paramsConfig: DefaultParams[String] = BaseParamsConfig): AccumuloCatalog =

    new AccumuloCatalog(sc, instance, attributes, paramsConfig)
}
