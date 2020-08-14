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

package geotrellis.store.s3

import geotrellis.store._

import software.amazon.awssdk.services.s3.S3Client


object S3LayerMover {
  def apply(
    attributeStore: AttributeStore,
    bucket: String,
    keyPrefix: String,
    s3Client: => S3Client = S3ClientProducer.get()
  ): LayerMover[LayerId] =
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier(attributeStore, bucket, keyPrefix, s3Client),
      layerDeleter = S3LayerDeleter(attributeStore, s3Client)
    )

  def apply(
    bucket: String,
    keyPrefix: String,
    destBucket: String,
    destKeyPrefix: String,
    s3Client: => S3Client
  ): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix, s3Client)
    new GenericLayerMover[LayerId](
      layerCopier  = S3LayerCopier(attributeStore, destBucket, destKeyPrefix, s3Client),
      layerDeleter = S3LayerDeleter(attributeStore, s3Client)
    )
  }

  def apply(
    bucket: String,
    keyPrefix: String,
    s3Client: => S3Client
  ): LayerMover[LayerId] = {
    val attributeStore = S3AttributeStore(bucket, keyPrefix, s3Client)
    new GenericLayerMover[LayerId](
      layerCopier    = S3LayerCopier(attributeStore, bucket, keyPrefix, s3Client),
      layerDeleter   = S3LayerDeleter(attributeStore, s3Client)
    )
  }

  def apply(attributeStore: S3AttributeStore): LayerMover[LayerId] =
    new GenericLayerMover[LayerId](
      layerCopier    = S3LayerCopier(attributeStore),
      layerDeleter   = S3LayerDeleter(attributeStore, attributeStore.client)
    )
}
