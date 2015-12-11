package geotrellis.spark.io.s3

import geotrellis.spark.utils.cache.Cache
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

class S3LayerReindexer[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container](
   bucket: String,
   prefix: String,
   keyIndexMethod: KeyIndexMethod[K],
   getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container]) extends LayerReindexer[LayerId, K, Container] {

  lazy val attributeStore = S3AttributeStore(bucket, prefix)
  lazy val layerReader    = new S3LayerReader[K, V, Container](attributeStore, new S3RDDReader[K, V], getCache)
  lazy val layerDeleter   = S3LayerDeleter(attributeStore)
  lazy val layerCopier    = new S3LayerCopier[K, V, Container](attributeStore, bucket, prefix)
  val layerMover          = S3LayerMover(attributeStore, layerCopier, layerDeleter)

  def tmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-tmp")
}
