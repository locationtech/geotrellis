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

import geotrellis.layers.{ContextCollection, LayerId}
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.layers.io.index._
import geotrellis.util._

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.S3Client

import spray.json.JsonFormat

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3CollectionLayerReader(
  val attributeStore: AttributeStore,
  val getClient: () => S3Client = S3ClientProducer.get
) extends CollectionLayerReader[LayerId] {

  def collectionReader: S3CollectionReader = new S3CollectionReader(getClient)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], filterIndexOnly: Boolean) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
      case e: NoSuchBucketException => throw new LayerReadError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key

    val queryKeyBounds = rasterQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))
    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = (index: BigInt) => makePath(prefix, Index.encode(index, maxWidth))
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val seq = collectionReader.read[K, V](bucket, keyPath, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))

    new ContextCollection(seq, layerMetadata)
  }
}

object S3CollectionLayerReader {
  def apply(attributeStore: AttributeStore): S3CollectionLayerReader =
    new S3CollectionLayerReader(attributeStore, S3ClientProducer.get)

  def apply(bucket: String, prefix: String, getClient: () => S3Client = S3ClientProducer.get): S3CollectionLayerReader =
    apply(new S3AttributeStore(bucket, prefix, getClient))
}
