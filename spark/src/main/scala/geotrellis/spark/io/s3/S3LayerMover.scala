package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.LayerId
import spray.json.JsonFormat

import scala.reflect.ClassTag

object S3LayerMover {
  def apply(attrStore: AttributeStore[JsonFormat],
            lCopier   : LayerCopier[LayerId],
            lDeleter  : LayerDeleter[LayerId]): LayerMover[LayerId] =
    new LayerMover[LayerId] {
      val attributeStore = attrStore
      val layerCopier    = lCopier
      val layerDeleter   = lDeleter
    }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (attributeStore: AttributeStore[JsonFormat], bucket: String, keyPrefix: String)
    (implicit cons: ContainerConstructor[K, V, Container[K]]): LayerMover[LayerId] = {
    apply(
      attrStore = attributeStore,
      lCopier    = S3LayerCopier[K, V, Container](attributeStore, bucket, keyPrefix),
      lDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String)
    (implicit cons: ContainerConstructor[K, V, Container[K]]): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attrStore = attributeStore,
      lCopier    = S3LayerCopier[K, V, Container](attributeStore, destBucket, destKeyPrefix),
      lDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, Container[_]]
    (bucket: String, keyPrefix: String)(implicit cons: ContainerConstructor[K, V, Container[K]]): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attrStore = attributeStore,
      lCopier    = S3LayerCopier[K, V, Container](attributeStore, bucket, keyPrefix),
      lDeleter   = S3LayerDeleter(attributeStore)
    )
  }
}

