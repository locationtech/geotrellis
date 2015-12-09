package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.spark.LayerId
import spray.json.JsonFormat

object S3LayerMover {
  def apply(attributeStore: AttributeStore[JsonFormat],
            layerCopier   : LayerCopier[LayerId],
            layerDeleter  : LayerDeleter[LayerId]): LayerMover[LayerId] =
    new LayerMover(attributeStore, layerCopier, layerDeleter)

  def apply(attributeStore: AttributeStore[JsonFormat],
                    bucket: String,
                 keyPrefix: String): LayerMover[LayerId] = {
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier(attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply(bucket: String,
         keyPrefix: String,
        destBucket: String,
     destKeyPrefix: String): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier(attributeStore, destBucket, destKeyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply(bucket: String, keyPrefix: String): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    apply(
      attributeStore = attributeStore,
      layerCopier    = S3LayerCopier(attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }
}

