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

package geotrellis.store.s3

import geotrellis.store._

import java.net.URI

/**
 * Provides [[S3LayerReader]] instance for URI with `s3` scheme.
 * The uri represents S3 bucket an prefix of catalog root.
 *  ex: `s3://<bucket>/<prefix-to-catalog>`
 */
class S3CollectionLayerProvider() extends AttributeStoreProvider with ValueReaderProvider with CollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "s3") true else false
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val s3Uri = new AmazonS3URI(uri)
    val prefix =
      Option(s3Uri.getKey()) match {
        case Some(s) => s
        case None => ""
      }
    new S3AttributeStore(bucket = s3Uri.getBucket(), prefix = prefix, S3ClientProducer.get())
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    new S3ValueReader(store, S3ClientProducer.get())
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): CollectionLayerReader[LayerId] = {
    new S3CollectionLayerReader(store)
  }
}
