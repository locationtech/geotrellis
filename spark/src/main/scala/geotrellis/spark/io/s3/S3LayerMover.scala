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
    layerDeleter.delete(from)
  }
}

object S3LayerMover {
  def apply(attributeStore: AttributeStore[JsonFormat],
            layerCopier   : LayerCopier[LayerId],
            layerDeleter  : LayerDeleter[LayerId]): S3LayerMover =
    new S3LayerMover(attributeStore, layerCopier, layerDeleter)

  def apply(attributeStore: AttributeStore[JsonFormat],
                    bucket: String,
                 keyPrefix: String): S3LayerMover = {
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier(attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply(bucket: String,
         keyPrefix: String,
        destBucket: String,
     destKeyPrefix: String): S3LayerMover = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier(attributeStore, destBucket, destKeyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply(bucket: String, keyPrefix: String): S3LayerMover = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier(attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }
}

