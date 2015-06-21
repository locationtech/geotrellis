package geotrellis.spark.io.parquet

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import org.apache.hadoop.fs.Path
import spray.json.JsonFormat
import scala.reflect._
import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain, AWSCredentialsProvider}
import com.amazonaws.retry.PredefinedRetryPolicies

object ParquetRasterCatalog {

  def apply(rootPath: String)(implicit sc: SparkContext): ParquetRasterCatalog = {
    // Todo -- figure out how to store attribute catalog
    val attributeStore = new HadoopAttributeStore(sc.hadoopConfiguration, new Path(rootPath, "attributes"))
    new ParquetRasterCatalog(rootPath, attributeStore)
  }
}

class ParquetRasterCatalog(rootPath: String, as: HadoopAttributeStore)(implicit sc: SparkContext) extends AttributeCaching[HadoopLayerMetaData] {

  val attributeStore: HadoopAttributeStore = as

  val rasterDataPath = s"$rootPath/rasterData"


  // def read[K: RasterRDDReader: JsonFormat: ClassTag](layerId: LayerId, rasterQuery: RasterRDDQuery[K], numPartitions: Int = sc.defaultParallelism): RasterRDD[K] = {
  //   val metadata  = getLayerMetadata(layerId)
  //   val keyBounds = getLayerKeyBounds(layerId)
  //   val index     = getLayerKeyIndex(layerId)

  //   val queryBounds = rasterQuery(metadata.rasterMetaData, keyBounds)
  //   implicitly[RasterRDDReader[K]].read(s3client, metadata, keyBounds, index, numPartitions)(layerId, queryBounds)
  // }

  // def query[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): BoundRasterRDDQuery[K] ={
  //   new BoundRasterRDDQuery[K](new RasterRDDQuery[K], read(layerId, _, sc.defaultParallelism))
  // }

  // def query[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, numPartitions: Int): BoundRasterRDDQuery[K] = {
  //   new BoundRasterRDDQuery[K](new RasterRDDQuery[K], read(layerId, _, numPartitions))
  // }

  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        rdd.persist()

        val md = HadoopLayerMetaData(
          keyClass = classTag[K].toString,
          rasterMetaData = rdd.metaData,
          path = new Path(rasterDataPath))


        val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
        val index = {
          // Expanding spatial bounds? To allow multi-stage save?
          val indexKeyBounds = {
            val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        val rddWriter = implicitly[RasterRDDWriter[K]]
        rddWriter.write(rasterDataPath, index, true)(layerId, rdd)

        setLayerMetadata(layerId, md)
        setLayerKeyBounds(layerId, keyBounds)
        setLayerKeyIndex(layerId, index)

        rdd.unpersist(blocking = false)
      }
    }

  // def tileReader[K: TileReader: JsonFormat: ClassTag](layerId: LayerId): K => Tile = {
  //   val metadata  = getLayerMetadata(layerId)
  //   val keyBounds = getLayerKeyBounds(layerId)
  //   val index     = getLayerKeyIndex(layerId)
  //   implicitly[TileReader[K]].read(s3client(), layerId, metadata, index, keyBounds)(_)
  // }
}
