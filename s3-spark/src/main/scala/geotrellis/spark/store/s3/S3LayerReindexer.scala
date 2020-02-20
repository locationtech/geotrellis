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

package geotrellis.spark.store.s3

import geotrellis.store._
import geotrellis.store.s3._
import geotrellis.spark.store._

import org.apache.spark.SparkContext
import software.amazon.awssdk.services.s3.S3Client

object S3LayerReindexer {
  def apply(attributeStore: S3AttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReader  = S3LayerReader(attributeStore, attributeStore.client)
    val layerWriter  = S3LayerWriter(attributeStore)
    val layerDeleter = S3LayerDeleter(attributeStore, attributeStore.client)
    val layerCopier  = S3LayerCopier(attributeStore)

    GenericLayerReindexer(attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)
  }

  def apply(
    bucket: String,
    prefix: String,
    s3Client: => S3Client
  )(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(S3AttributeStore(bucket, prefix, s3Client))
}
