package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.LayerId
import spray.json.JsonFormat

import scala.reflect.ClassTag

object S3LayerMover {
  def apply(attributeStore: AttributeStore[JsonFormat],
            layerCopier   : LayerCopier[LayerId],
            layerDeleter  : LayerDeleter[LayerId]): LayerMover[LayerId] =
    new LayerMover(attributeStore, layerCopier, layerDeleter)

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (attributeStore: AttributeStore[JsonFormat], bucket: String, keyPrefix: String)
    (implicit cons: ContainerConstructor[K, V, Container[K]]): LayerMover[LayerId] = {
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier[K, V, Container](attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String)
    (implicit cons: ContainerConstructor[K, V, Container[K]]): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier[K, V, Container](attributeStore, destBucket, destKeyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (bucket: String, keyPrefix: String)(implicit cons: ContainerConstructor[K, V, Container[K]]): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier[K, V, Container](attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }
}

