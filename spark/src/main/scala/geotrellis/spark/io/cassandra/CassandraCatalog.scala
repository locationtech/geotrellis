package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.raster._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.util.{Failure, Success, Try}
import geotrellis.spark.op.stats._

import com.datastax.spark.connector.cql.CassandraConnector

class CassandraCatalog(sc: SparkContext,
  val connector: CassandraConnector,
  val keyspace: String,
  val metaDataCatalog: CassandraMetaDataCatalog, 
  val paramsConfig: DefaultParams[String]
) extends Catalog {
  type Params = String
  type SupportedKey[K] = CassandraDriver[K]

  def paramsFor[K: SupportedKey: ClassTag](id: LayerId): String =
    paramsConfig.paramsFor[K](id) match {
      case Some(params) => params
      case None => sys.error(s"Default Params for '$id' not found.")
    }

  def load[K: CassandraDriver: ClassTag](id: LayerId, metaData: RasterMetaData, table: String, filters: FilterSet[K]): RasterRDD[K] = {
    val driver = implicitly[CassandraDriver[K]]
    driver.load(sc, keyspace)(id, metaData, table, filters)
  }

  def loadTile[K: CassandraDriver: ClassTag](id: LayerId, key: K): Tile = {
    val (metaData, table) = metaDataCatalog.load(id)
    loadTile(id, metaData, table, key)
  }

  def loadTile[K: CassandraDriver: ClassTag](id: LayerId, metaData: LayerMetaData, table: String, key: K): Tile = {
    val driver = implicitly[CassandraDriver[K]]
    driver.loadTile(connector)(id, metaData.rasterMetaData, table, key)
  }

  def save[K: SupportedKey : ClassTag](id: LayerId, table: String, rdd: RasterRDD[K], clobber: Boolean): Unit = {
    val driver = implicitly[CassandraDriver[K]]
    rdd.persist()
    driver.save(sc, keyspace)(id, rdd, table, clobber)

    val metaData = LayerMetaData(
      rasterMetaData = rdd.metaData,
      keyClass = classTag[K].toString,
      histogram = Some(rdd.histogram)
    )
    metaDataCatalog.save(id, table: Params, metaData, clobber)
    rdd.unpersist(blocking = false)
  }
}

object CassandraCatalog {

  /** Use val as base in Builder pattern to make your own table mappings. */
  val BaseParamsConfig = new DefaultParams[String](Map.empty, Map.empty)

  def apply(
    sc: SparkContext,
    keyspace: String,
    metaDataCatalog: CassandraMetaDataCatalog,
    paramsConfig: DefaultParams[String] = BaseParamsConfig): CassandraCatalog =

    new CassandraCatalog(sc, CassandraConnector(sc.getConf), keyspace, metaDataCatalog, paramsConfig)
}
