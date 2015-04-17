package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import spray.json.JsonFormat
import scala.reflect._
import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentialsProvider}

case class S3RasterCatalogConfig(  
  /** Creates a subdirectory path based on a layer id. */
  layerDataDir: LayerId => String,
  credentialsProvider: AWSCredentialsProvider
) 

object S3RasterCatalogConfig {
  val DEFAULT =
    S3RasterCatalogConfig(
      layerDataDir = { layerId: LayerId => s"${layerId.name}/${layerId.zoom}" },
      credentialsProvider = new DefaultAWSCredentialsProviderChain()
    )
}

object S3RasterCatalog {
  lazy val BaseParams = new DefaultParams[String](Map.empty.withDefaultValue(""), Map.empty)

  def apply(bucket: String, 
            rootPath: String,
            paramsConfig: DefaultParams[String] = BaseParams,
            catalogConfig: S3RasterCatalogConfig = S3RasterCatalogConfig.DEFAULT)
          (implicit sc: SparkContext): S3RasterCatalog = {    
    
    val attributeStore = new S3AttributeStore(catalogConfig.credentialsProvider, bucket, catalogConfig.layerDataDir)    
    new S3RasterCatalog(bucket, rootPath, attributeStore, BaseParams, S3RasterCatalogConfig.DEFAULT)
  }
}

class S3RasterCatalog(
    bucket: String,
    rootPath: String,
    val attributeStore: S3AttributeStore,    
    paramsConfig: DefaultParams[String],
    catalogConfig: S3RasterCatalogConfig)
  (implicit sc: SparkContext) {

  def reader[K: RasterRDDReaderProvider](): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData = attributeStore.read[S3LayerMetaData](layerId, "metaData")
        implicitly[RasterRDDReaderProvider[K]].reader(catalogConfig.credentialsProvider, metaData).read(layerId, filterSet)
      }      
    }

  def writer[K: SpatialComponent: RasterRDDWriterProvider: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K]): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, clobber = true)

  def writer[K: SpatialComponent: RasterRDDWriterProvider: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir, clobber = true)

  // TODO: there has to be better than this
  def writer[K: SpatialComponent: RasterRDDWriterProvider: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        writer[K](keyIndexMethod, paramsConfig.paramsFor[K](layerId).getOrElse(""), true).write(layerId, rdd)
      }
    }
  
  def writer[K: SpatialComponent: RasterRDDWriterProvider: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        rdd.persist()

        val layerPath = 
          if (subDir != "")
            s"${rootPath}/${subDir}/${catalogConfig.layerDataDir(layerId)}"
          else
            s"${rootPath}/${catalogConfig.layerDataDir(layerId)}"

        val md = S3LayerMetaData(
            layerId = layerId,
            keyClass = classTag[K].toString,
            rasterMetaData = rdd.metaData,
            bucket = bucket,
            key = layerPath)

        // Note: this is a waste to do for spatial raster, we can derive them easily from RasterMetaData
        val boundable = implicitly[Boundable[K]]
        val keyBounds = rdd
          .map{ case (k, tile) => KeyBounds(k, k) }
          .reduce { boundable.combine }

        val index = {
          // Expanding spatial bounds? To allow multi-stage save?
          val indexKeyBounds = {
            val imin = keyBounds.minKey.updateSpatialComponent(SpatialKey(0, 0))
            val imax = keyBounds.maxKey.updateSpatialComponent(SpatialKey(rdd.metaData.tileLayout.layoutCols - 1, rdd.metaData.tileLayout.layoutRows - 1))
            KeyBounds(imin, imax)
          }
          keyIndexMethod.createIndex(indexKeyBounds)
        }

        attributeStore.write(layerId, "keyIndex", index)
        attributeStore.write[KeyBounds[K]](layerId, "keyBounds", keyBounds)
        attributeStore.write[S3LayerMetaData](layerId, "metaData", md)

        val rddWriter = implicitly[RasterRDDWriterProvider[K]]
          .writer(catalogConfig.credentialsProvider, bucket, layerPath, clobber)        
        
        rddWriter.write(layerId, rdd)
        rdd.unpersist(blocking = false)
      }
    }

  // def tileReader[K: TileReaderProvider](layerId: LayerId): Reader[K, Tile] = {
  //   val layerMetaData = metaDataCatalog.read(layerId)
  //   implicitly[TileReaderProvider[K]].reader(layerMetaData)
  // }
}
