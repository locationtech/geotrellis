package geotrellis.spark.io.s3

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import geotrellis.util._

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object S3LayerMover {
  def apply(attributeStore: AttributeStore, bucket: String, keyPrefix: String): LayerMover[LayerId] =
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier(attributeStore, bucket, keyPrefix),
      layerDeleter = S3LayerDeleter(attributeStore)
    )

  def apply(bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier(attributeStore, destBucket, destKeyPrefix),
      layerDeleter = S3LayerDeleter(attributeStore)
    )
  }

  def apply(bucket: String, keyPrefix: String): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix)
    new GenericLayerMover[LayerId](
      layerCopier    = S3LayerCopier(attributeStore, bucket, keyPrefix),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
  }

  def apply(attributeStore: S3AttributeStore): LayerMover[LayerId] =
    new GenericLayerMover[LayerId](
      layerCopier    = S3LayerCopier(attributeStore),
      layerDeleter   = S3LayerDeleter(attributeStore)
    )
}
