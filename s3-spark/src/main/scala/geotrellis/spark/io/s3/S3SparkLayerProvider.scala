/*
 * Copyright 2017 Azavea
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

import geotrellis.layers._
import geotrellis.store.s3._
import geotrellis.spark.store._

import org.apache.spark._

import software.amazon.awssdk.services.s3.S3Client

import java.net.URI


/**
 * Provides [[S3LayerReader]] instance for URI with `s3` scheme.
 * The uri represents S3 bucket an prefix of catalog root.
 *  ex: `s3://<bucket>/<prefix-to-catalog>`
 */
class S3SparkLayerProvider extends S3CollectionLayerProvider with LayerReaderProvider with LayerWriterProvider {
  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId] = {
    new S3LayerReader(store, getClient)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId] = {
    // TODO: encoder ACL changes in putObjectModifier
    val s3Uri = new AmazonS3URI(uri)
    new S3LayerWriter(store, bucket = s3Uri.getBucket(), keyPrefix = s3Uri.getKey(), identity, getClient)
  }
}
