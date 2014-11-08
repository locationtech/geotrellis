package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}

class AccumuloCatalog(sc: SparkContext, instance: AccumuloInstance, 
  val metaDataCatalog: AccumuloMetaDataCatalog, 
  val catalogConfig: AccumuloCatalog.Config
) extends Catalog {
  type Params = String
  type SupportedKey[K] = AccumuloDriver[K]

  def paramsFor[K: SupportedKey: ClassTag](layerId: LayerId): String = 
    catalogConfig.tableNameFor[K](layerId)

  def load[K: AccumuloDriver: ClassTag](metaData: LayerMetaData, table: String, filters: FilterSet[K]): Try[RasterRDD[K]] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.load(sc, instance)(metaData, table, filters)
  }

  protected def save[K: AccumuloDriver: ClassTag](rdd: RasterRDD[K], layerMetaData: LayerMetaData, table: String, clobber: Boolean): Try[Unit] = {
    val driver = implicitly[AccumuloDriver[K]]
    driver.save(sc, instance)(layerMetaData.id, rdd, table, clobber)
  }
}

object AccumuloCatalog {

  /** 
   * Configuration that allows you to specify rules for selecting target based on RasterRDD key type and LayerId table on save.
   * Rules will be applies first, then the defaults. If no mapping can be found, exception will be thrown.
   *
   * ex: AccumuloCatalog.BaseConfig
   *     .defaultTable[SpaceTimeKey]("rainforest_timelapse")
   *     .defaultTable[SpatialKey]("satellite")    
   *     .ruleTable[SpatialKey]{
   *       case LayerId(name, zoom) if name.startsWith("NLCD") =>  s"satellite_nlcd_$zoom"
   *     }
   * 
   */
  class Config(
    ruleTables:   Map[ClassTag[_], PartialFunction[LayerId, String]],
    defaultTables: Map[ClassTag[_], String]
  ){   
    /** Default tables mappings based only on the key type, if no layer rule is found */
    def defaultTable[K: ClassTag](tableName: String) = 
      new Config(ruleTables, defaultTables updated (classTag[K],tableName))

    /** Provide a LayerId based rule to use for specific key tupe */  
    def ruleTable[K: ClassTag](rule: PartialFunction[LayerId, String]) = 
      new Config(ruleTables updated (classTag[K], rule), defaultTables)    


    def tableNameFor[K: ClassTag](layerId: LayerId) = {
      def default = defaultTables.get(classTag[K]) match {
        case Some(tableName) => 
          tableName
        case None => 
          sys.error(s"Can not save layer '$layerId'. Default table mapping not provided, please specify a table name as argument.")
      }
            
      ruleTables.get(classTag[K]) match {
        case Some(rule) => 
          if(rule isDefinedAt layerId) rule(layerId) else default
        case None => 
          default
      }      
    }
  }

  /** Use val as base in Builder pattern to make your own table mappings. */
  val BaseConfig = new Config(Map.empty, Map.empty)

  def apply(
    sc: SparkContext,
    instance: AccumuloInstance,
    metaDataCatalog: AccumuloMetaDataCatalog,
    catalogConfig: AccumuloCatalog.Config = BaseConfig): AccumuloCatalog =
    new AccumuloCatalog(sc, instance, metaDataCatalog, catalogConfig)
}
