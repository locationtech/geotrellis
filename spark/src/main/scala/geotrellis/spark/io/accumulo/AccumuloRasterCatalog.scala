package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.raster._
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
)(implicit sc: SparkContext) extends AttributeCaching[AccumuloLayerMetaData] {

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, query: RasterRDDQuery[K]): RasterRDD[K] = {
    try {
      val metadata  = getLayerMetadata(layerId)
      val keyBounds = getLayerKeyBounds(layerId)
      val index     = getLayerKeyIndex(layerId)

      implicitly[RasterRDDReader[K]]
        .read(instance, metadata, keyBounds, index)(layerId, query(metadata.rasterMetaData, keyBounds))
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
        val index = {
          val indexKeyBounds = {
            val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        setLayerMetadata(layerId, md)
        setLayerKeyBounds(layerId, keyBounds)
        setLayerKeyIndex(layerId, index)

        val rddWriter = implicitly[RasterRDDWriter[K]]
        rddWriter.write(instance, md, keyBounds, index)(layerId, rdd, strategy)
        rdd.unpersist(blocking = false)
      }
    }
  }

  def tileReader[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): Reader[K, Tile] =
    new Reader[K, Tile] {
      val readTile = {
        val metadata  = getLayerMetadata(layerId)
        val keyBounds = getLayerKeyBounds(layerId)                
        val index     = getLayerKeyIndex(layerId)
        implicitly[TileReader[K]].read(instance, layerId, metadata, index)(_)        
      }

      def read(key: K) = readTile(key)
    }  
}
