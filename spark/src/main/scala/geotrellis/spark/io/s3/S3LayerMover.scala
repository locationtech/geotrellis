package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.LayerId
import geotrellis.spark.io.index.KeyIndex

import spray.json.JsonFormat
import scala.reflect.ClassTag

object S3LayerMover {
  def apply[
    K: JsonFormat: ClassTag, V: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
    (attributeStore: AttributeStore[JsonFormat], bucket: String, keyPrefix: String): LayerMover[LayerId] = {
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier[K, V, M, I](attributeStore, bucket, keyPrefix),
      layerDeleter = S3LayerDeleter(attributeStore)
    )
  }

  def apply[
    K: JsonFormat: ClassTag, V: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
    (bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier[K, V, M, I](attributeStore, destBucket, destKeyPrefix),
      layerDeleter = S3LayerDeleter(attributeStore)
    )
  }

  def apply[
    K: JsonFormat: ClassTag, V: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
    (bucket: String, keyPrefix: String): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    new GenericLayerMover[LayerId](
      layerCopier    = S3LayerCopier[K, V, M, I](attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }
}
