package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import geotrellis.spark.{Boundable, KeyBounds, LayerId}
import org.apache.spark.SparkContext
import spray.json.DefaultJsonProtocol._
import spray.json.JsonFormat

import scala.reflect.ClassTag

class S3LayerMover(attributeStore: AttributeStore[JsonFormat],
                   layerCopier   : LayerCopier[LayerId],
                   layerDeleter  : LayerDeleter[LayerId]) extends LayerMover[LayerId] {
  def move(from: LayerId, to: LayerId): Unit = {
    layerCopier.copy(from, to)
    attributeStore.copy(from, to)
    layerDeleter.delete(from)
  }
}

object S3LayerMover {
  def apply(attributeStore: AttributeStore[JsonFormat],
            layerCopier   : LayerCopier[LayerId],
            layerDeleter  : LayerDeleter[LayerId]): S3LayerMover =
    new S3LayerMover(attributeStore, layerCopier, layerDeleter)

  def apply[K: Boundable: JsonFormat: ClassTag]
  (bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String)(implicit sc: SparkContext): S3LayerMover = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier[K](attributeStore, destBucket, destKeyPrefix),
      layerDeleter   = S3LayerDeleter[K](attributeStore)
    )
  }

  def apply[K: Boundable: JsonFormat: ClassTag]
  (bucket: String, keyPrefix: String)(implicit sc: SparkContext): S3LayerMover = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier[K](attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter[K](attributeStore)
    )
  }
}
