/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.s3

import geotrellis.layers.LayerId
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
