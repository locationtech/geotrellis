package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.LayerId

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object S3LayerMover {
  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
    (attributeStore: AttributeStore[JsonFormat], bucket: String, keyPrefix: String)
    (implicit bridge: Bridge[(RDD[(K, V)], M), C]): LayerMover[LayerId] = {
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier[K, V, M, C](attributeStore, bucket, keyPrefix),
      layerDeleter = S3LayerDeleter(attributeStore)
    )
  }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
    (bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String)
    (implicit bridge: Bridge[(RDD[(K, V)], M), C]): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier[K, V, M, C](attributeStore, destBucket, destKeyPrefix),
      layerDeleter = S3LayerDeleter(attributeStore)
    )
  }

  def apply[K: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
    (bucket: String, keyPrefix: String)(implicit bridge: Bridge[(RDD[(K, V)], M), C]): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    new GenericLayerMover[LayerId](
      layerCopier    = S3LayerCopier[K, V, M, C](attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }
}
