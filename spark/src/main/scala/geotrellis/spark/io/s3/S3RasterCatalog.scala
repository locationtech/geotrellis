package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import spray.json.JsonFormat

import scala.reflect._

object S3RasterCatalog {
  def layerPath(layerId: LayerId) =
    s"${layerId.name}/${layerId.zoom}"  

  def apply(bucket: String)(implicit sc: SparkContext): S3RasterCatalog =
    apply(bucket, "", () => S3Client.default)

  def apply(bucket: String, rootPath: String, s3client: () => S3Client = () => S3Client.default)
    (implicit sc: SparkContext): S3RasterCatalog = {
    
    val attributeStore = new S3AttributeStore(bucket, rootPath)
    new S3RasterCatalog(bucket, rootPath, attributeStore, s3client)
  }
}

class S3RasterCatalog(
  bucket: String,
  rootPath: String,
  val attributeStore: S3AttributeStore,    
  s3client: ()=>S3Client)
(implicit sc: SparkContext) extends AttributeCaching[S3LayerMetaData] {
  import S3RasterCatalog._

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, rasterQuery: RasterRDDQuery[K], numPartitions: Int = sc.defaultParallelism): RasterRDD[K] = {
    try {
      val metadata  = getLayerMetadata(layerId)
      val keyBounds = getLayerKeyBounds(layerId)                
      val index     = getLayerKeyIndex(layerId)

      val queryBounds = rasterQuery(metadata.rasterMetaData, keyBounds)
      implicitly[RasterRDDReader[K]].read(attributeStore ,s3client, metadata, keyBounds, index, numPartitions)(layerId, queryBounds)
    } catch {
      case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId)
    }
  }

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, numPartitions: Int): RasterRDD[K] =
    query[K](layerId, numPartitions).toRDD

  def read[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): RasterRDD[K] =
    query[K](layerId, sc.defaultParallelism).toRDD

  def query[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId): BoundRasterRDDQuery[K] ={
    new BoundRasterRDDQuery[K](new RasterRDDQuery[K], read(layerId, _, sc.defaultParallelism))
  }

  def query[K: RasterRDDReader: Boundable: JsonFormat: ClassTag](layerId: LayerId, numPartitions: Int): BoundRasterRDDQuery[K] = {
    new BoundRasterRDDQuery[K](new RasterRDDQuery[K], read(layerId, _, numPartitions))
  }
}


