package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import spray.json.JsonFormat
import scala.reflect._
import com.amazonaws.auth.{AWSCredentials, DefaultAWSCredentialsProviderChain, AWSCredentialsProvider}

object S3RasterCatalog {  
  def defaultS3Client = 
    () => new AmazonS3Client(new DefaultAWSCredentialsProviderChain) 
  
  private def layerPath(layerId: LayerId) = 
    s"${layerId.name}/${layerId.zoom}"  

  def apply(bucket: String, rootPath: String, s3client: ()=>S3Client = defaultS3Client)
    (implicit sc: SparkContext): S3RasterCatalog = {
    
    val attributeStore = new S3AttributeStore(s3client(), bucket, rootPath)
    new S3RasterCatalog(bucket, rootPath, attributeStore, s3client)
  }
}

class S3RasterCatalog(
  bucket: String,
  rootPath: String,
  val attributeStore: S3AttributeStore,    
  s3client: ()=>S3Client)
(implicit sc: SparkContext) {
  import S3RasterCatalog._

  def reader[K: RasterRDDReader: JsonFormat: ClassTag](): FilterableRasterRDDReader[K] =
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {
        val metaData  = attributeStore.read[S3LayerMetaData](layerId, "metaData")
        val keyBounds = attributeStore.read[KeyBounds[K]](layerId, "keyBounds")
        val index     = attributeStore.read[KeyIndex[K]](layerId, "keyIndex")
        implicitly[RasterRDDReader[K]].read(s3client, metaData, keyBounds, index)(layerId, filterSet)
      }      
    }

  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K]): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, clobber = true)

  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, subDir, clobber = true)

  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    writer[K](keyIndexMethod, "", clobber = true)  
  
  def writer[K: SpatialComponent: RasterRDDWriter: Boundable: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], subDir: String, clobber: Boolean): Writer[LayerId, RasterRDD[K]] =
    new Writer[LayerId, RasterRDD[K]] {
      def write(layerId: LayerId, rdd: RasterRDD[K]): Unit = {
        rdd.persist()

        val path = 
          if (subDir != "")
            s"${rootPath}/${subDir}/${layerPath(layerId)}"
          else
            s"${rootPath}/${layerPath(layerId)}"

        val md = S3LayerMetaData(
            layerId = layerId,
            keyClass = classTag[K].toString,
            rasterMetaData = rdd.metaData,
            bucket = bucket,
            key = path)

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

        val rddWriter = implicitly[RasterRDDWriter[K]]
          .write(s3client, bucket, path, index, clobber)(layerId, rdd)

        rdd.unpersist(blocking = false)
      }
    }

  // def tileReader[K: TileReaderProvider](layerId: LayerId): Reader[K, Tile] = {
  //   val layerMetaData = metaDataCatalog.read(layerId)
  //   implicitly[TileReaderProvider[K]].reader(layerMetaData)
  // }
}
