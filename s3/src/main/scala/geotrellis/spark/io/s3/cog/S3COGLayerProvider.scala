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

package geotrellis.spark.io.s3.cog

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.s3._
import org.apache.spark._
import com.amazonaws.services.s3.AmazonS3URI
import java.net.URI

import geotrellis.layers.LayerId
import geotrellis.layers.io.cog.{COGCollectionLayerReader, COGCollectionLayerReaderProvider, COGValueReader, COGValueReaderProvider}

/**
 * Provides [[S3LayerReader]] instance for URI with `s3` scheme.
 * The uri represents S3 bucket an prefix of catalog root.
 *  ex: `s3://<bucket>/<prefix-to-catalog>`
 */
class S3COGLayerProvider extends AttributeStoreProvider
    with COGLayerReaderProvider with COGLayerWriterProvider with COGValueReaderProvider with COGCollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "s3") true else false
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val s3Uri = new AmazonS3URI(uri)
    val prefix =
      Option(s3Uri.getKey) match {
        case Some(s) => s
        case None => ""
      }
    new S3AttributeStore(bucket = s3Uri.getBucket, prefix = prefix)
  }

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): COGLayerReader[LayerId] = {
    new S3COGLayerReader(store)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): COGLayerWriter = {
    // TODO: encoder ACL changes in putObjectModifier
    val s3Uri = new AmazonS3URI(uri)
    new S3COGLayerWriter(store, bucket = s3Uri.getBucket, keyPrefix = s3Uri.getKey)
  }

  def valueReader(uri: URI, store: AttributeStore): COGValueReader[LayerId] = {
    new S3COGValueReader(store)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): COGCollectionLayerReader[LayerId] = {
    new S3COGCollectionLayerReader(store)
  }
}
