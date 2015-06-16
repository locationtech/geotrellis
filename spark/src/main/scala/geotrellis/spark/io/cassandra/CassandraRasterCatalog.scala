package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.op.stats._
import geotrellis.raster._

import org.apache.spark.SparkContext

import com.typesafe.config.{ConfigFactory,Config}

import scala.reflect._
import spray.json._

object CassandraRasterCatalog {
  def apply()(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog = {
    val attributesTable = ConfigFactory.load().getString("geotrellis.cassandra.attributesCatalog")
    apply(attributesTable)
  }

  def apply(attributesTable: String)(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog =
    apply(CassandraAttributeStore(attributesTable))

  def apply(attributeStore: CassandraAttributeStore)(implicit session: CassandraSession, sc: SparkContext): CassandraRasterCatalog =
    new CassandraRasterCatalog(attributeStore)
}

// TODO no extends AttributeCaching[CassandraLayerMetaData] because no attributeStore
class CassandraRasterCatalog(
  attributeStore: CassandraAttributeStore
)(implicit session: CassandraSession, sc: SparkContext)  {

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, query: RasterRDDQuery[K]): RasterRDD[K] = {
    try {
      val metadata  = attributeStore.read[CassandraLayerMetaData](layerId, "metadata")
      val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
      val index     = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")

      implicitly[RasterRDDReader[K]]
        .read(metadata, keyBounds, index)(layerId, query(metadata.rasterMetaData, keyBounds))
        // .read(instance, metadata, keyBounds, index)(layerId, query(metadata.rasterMetaData, keyBounds))
    } catch {
      case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId)
    }
  }

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): RasterRDD[K] =
    query[K](layerId).toRDD

  def query[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): BoundRasterRDDQuery[K] =
    new BoundRasterRDDQuery[K](new RasterRDDQuery[K], read[K](layerId, _))

  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: Ordering: ClassTag](
    keyIndexMethod: KeyIndexMethod[K],
    tileTable: String
  ): Writer[LayerId, RasterRDD[K]] = {
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        // Persist since we are both calculating a histogram and saving tiles.
        rdd.persist()

        val md =
          CassandraLayerMetaData(
            rasterMetaData = rdd.metaData,
            keyClass = classTag[K].toString,
            tileTable = tileTable
          )

        val keyBounds =  implicitly[Boundable[K]].getKeyBounds(rdd)

        val index = {
          val indexKeyBounds = {
            val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        attributeStore.write(layerId, "metadata", md)
        attributeStore.write[KeyBounds[K]](layerId, "keyBounds", keyBounds)
        attributeStore.write(layerId, "keyIndex", index)

        val rddWriter = implicitly[RasterRDDWriter[K]]
        rddWriter.write(md, keyBounds, index)(layerId, rdd)

        rdd.unpersist(blocking = false)
      }
    }
  }

  def tileReader[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): Reader[K, Tile] =
    new Reader[K, Tile] {
      val readTile = {
        val metadata = attributeStore.read[CassandraLayerMetaData](layerId, "metadata")
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[TileReader[K]].read(layerId, metadata, index)(_)
      }

      def read(key: K) = readTile(key)
  }
}
