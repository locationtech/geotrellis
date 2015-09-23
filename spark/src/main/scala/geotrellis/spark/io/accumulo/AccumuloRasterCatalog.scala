package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.spark.io.s3.S3LayerMetaData
import org.apache.hadoop.fs.Path

import org.apache.spark.SparkContext

import com.typesafe.config.ConfigFactory

import scala.reflect._
import spray.json._

object AccumuloRasterCatalog {
  def apply()(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog = {
    /** The value is specified in reference.conf, applications can overwrite it in their application.conf */
    val attributesTable = ConfigFactory.load().getString("geotrellis.accumulo.catalog")
    apply(attributesTable)
  }

  def apply(attributesTable: String)(implicit instance: AccumuloInstance, sc: SparkContext): AccumuloRasterCatalog =
     new AccumuloRasterCatalog(instance, AccumuloAttributeStore(instance.connector, attributesTable))
}

class AccumuloRasterCatalog(
  instance: AccumuloInstance, 
  val attributeStore: AccumuloAttributeStore
)(implicit sc: SparkContext)  {

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, query: RasterRDDQuery[K]): RasterRDD[K] = {
    try {
      val metadata  = attributeStore.cacheRead[AccumuloLayerMetaData](layerId, Fields.layerMetaData)
      val keyBounds = attributeStore.cacheRead[KeyBounds[K]](layerId, Fields.keyBounds)
      val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](layerId, Fields.keyIndex)

      implicitly[RasterRDDReader[K]]
        .read(instance, metadata, keyBounds, keyIndex)(layerId, query(metadata.rasterMetaData, keyBounds))
    } catch {
      case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId)
    }
  }

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): RasterRDD[K] =
    query[K](layerId).toRDD

  def query[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): BoundRasterRDDQuery[K, RasterRDD] =
    new BoundRasterRDDQuery(new RasterRDDQuery[K], read[K](layerId, _))

  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: Ordering: ClassTag](
    keyIndexMethod: KeyIndexMethod[K],
    tileTable: String,
    strategy: AccumuloWriteStrategy = HdfsWriteStrategy(new Path("/geotrellis-ingest"))
  ): Writer[LayerId, RasterRDD[K]] = {
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        // Persist since we are both calculating a histogram and saving tiles.
        rdd.persist()

        val md =
          AccumuloLayerMetaData(
            rasterMetaData = rdd.metaData,            
            keyClass = classTag[K].toString,
            tileTable = tileTable
          )

        val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
        val keyIndex = {
          val indexKeyBounds = {
            val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        attributeStore.cacheWrite(layerId, Fields.layerMetaData, md)
        attributeStore.cacheWrite(layerId, Fields.keyBounds, keyBounds)
        attributeStore.cacheWrite(layerId, Fields.keyIndex, keyIndex)

        val rddWriter = implicitly[RasterRDDWriter[K]]
        rddWriter.write(instance, md, keyBounds, keyIndex)(layerId, rdd, strategy)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def tileReader[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): Reader[K, Tile] =
    new Reader[K, Tile] {
      val readTile = {
        val metadata  = attributeStore.cacheRead[AccumuloLayerMetaData](layerId, Fields.layerMetaData)
        val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](layerId, Fields.keyIndex)

        implicitly[TileReader[K]].read(instance, layerId, metadata, keyIndex)(_)
      }

      def read(key: K) = readTile(key)
    }  
}
