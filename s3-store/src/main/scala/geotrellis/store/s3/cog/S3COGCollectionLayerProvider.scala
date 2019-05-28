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

package geotrellis.store.s3.cog

import geotrellis.layers._
import geotrellis.layers.cog.{COGCollectionLayerReader, COGCollectionLayerReaderProvider, COGValueReader, COGValueReaderProvider}
import geotrellis.store.s3._

import software.amazon.awssdk.services.s3.S3Client

import java.net.URI


/**
 * Provides [[S3LayerReader]] instance for URI with `s3` scheme.
 * The uri represents S3 bucket an prefix of catalog root.
 *  ex: `s3://<bucket>/<prefix-to-catalog>`
 */
class S3COGCollectionLayerProvider extends AttributeStoreProvider
    with COGValueReaderProvider with COGCollectionLayerReaderProvider {

  val getClient = S3ClientProducer.get

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "s3") true else false
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    // Need to use an alternative to AmazonS3URI
    // https://github.com/aws/aws-sdk-java-v2/issues/860
    val s3Uri = new AmazonS3URI(uri)
    val prefix =
      Option(s3Uri.getKey()) match {
        case Some(s) => s
        case None => ""
      }
    new S3AttributeStore(bucket = s3Uri.getBucket(), prefix = prefix, getClient)
  }

  def valueReader(uri: URI, store: AttributeStore): COGValueReader[LayerId] = {
    new S3COGValueReader(store, getClient)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): COGCollectionLayerReader[LayerId] = {
    new S3COGCollectionLayerReader(store)
  }
}
