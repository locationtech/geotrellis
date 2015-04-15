package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark._
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

  def writer[K: RasterRDDWriterProvider: ClassTag](): Writer[LayerId, RasterRDD[K]] =
    writer[K](clobber = true)

  def writer[K: RasterRDDWriterProvider: ClassTag](subDir: String): Writer[LayerId, RasterRDD[K]] =
    writer[K](subDir, clobber = true)

  // TODO: there has to be better than this
  def writer[K: RasterRDDWriterProvider: ClassTag](clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        writer[K](paramsConfig.paramsFor[K](layerId).getOrElse(""), true).write(layerId, rdd)
      }
    }
  
  def writer[K: RasterRDDWriterProvider: ClassTag](subDir: String, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        val layerPath = 
          if (subDir != "")
            s"${rootPath}/${subDir}/${catalogConfig.layerDataDir(layerId)}"
          else
            s"${rootPath}/${catalogConfig.layerDataDir(layerId)}"

        val rddWriter = implicitly[RasterRDDWriterProvider[K]]
          .writer(catalogConfig.credentialsProvider, bucket, layerPath, clobber)
        
        val md = S3LayerMetaData(
            layerId = layerId,
            keyClass = classTag[K].toString,
            rasterMetaData = rdd.metaData,
            bucket = bucket,
            key = layerPath)

        rddWriter.write(layerId, rdd)
        attributeStore.write[S3LayerMetaData](layerId, "metaData", md)
        return;
      }
    }

  // def tileReader[K: TileReaderProvider](layerId: LayerId): Reader[K, Tile] = {
  //   val layerMetaData = metaDataCatalog.read(layerId)
  //   implicitly[TileReaderProvider[K]].reader(layerMetaData)
  // }
}
