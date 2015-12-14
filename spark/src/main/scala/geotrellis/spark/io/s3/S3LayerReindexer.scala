package geotrellis.spark.io.s3

import geotrellis.spark.utils.cache.Cache
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object S3LayerReindexer {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container[_]](
    bucket: String,
    prefix: String,
    keyIndexMethod: KeyIndexMethod[K],
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None,
    clobber: Boolean = true,
    oneToOne: Boolean = false)
   (implicit sc: SparkContext,
           cons: ContainerConstructor[K, V, Container[K]],
    containerEv: Container[K] => Container[K] with RDD[(K, V)]): LayerReindexer[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, prefix)
    val layerReader    = S3LayerReader[K, V, Container](bucket, prefix, getCache)
    val layerDeleter   = S3LayerDeleter(attributeStore)
    val layerWriter    = S3LayerWriter[K, V, Container](bucket, prefix, keyIndexMethod, clobber, oneToOne)

    val layerCopier = new SparkLayerCopier[S3LayerHeader, K, V, Container[K]](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: S3LayerHeader): S3LayerHeader =
        header.copy(bucket, key = makePath(prefix, s"${id.name}/${id.zoom}"))
    }

    val layerMover = S3LayerMover(attributeStore, layerCopier, layerDeleter)

    LayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}