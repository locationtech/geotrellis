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

import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.util._
import geotrellis.store.avro._
import geotrellis.store.index._
import geotrellis.store.s3._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.util._

import org.apache.spark.SparkContext
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._
import io.circe._
import cats.effect._

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3LayerReader(
  val attributeStore: AttributeStore,
  s3Client: => S3Client = S3ClientProducer.get(),
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
)(implicit sc: SparkContext)
  extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def rddReader: S3RDDReader = new S3RDDReader(s3Client, runtime)

  def read[
    K: AvroRecordCodec: Boundable: Decoder: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Decoder: Component[*, Bounds[K]]
  ](id: LayerId, tileQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
      case e: NoSuchBucketException => throw new LayerReadError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key

    val queryKeyBounds = tileQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))
    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = (index: BigInt) => makePath(prefix, Index.encode(index, maxWidth))
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val rdd = rddReader.read[K, V](bucket, keyPath, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema), Some(numPartitions))

    new ContextRDD(rdd, layerMetadata)
  }
}

object S3LayerReader {
  def apply(attributeStore: AttributeStore, s3Client: => S3Client)(implicit sc: SparkContext): S3LayerReader =
    new S3LayerReader(attributeStore, s3Client)

  def apply(bucket: String, prefix: String, s3Client: => S3Client)(implicit sc: SparkContext): S3LayerReader = {
    val attStore = new S3AttributeStore(bucket, prefix, s3Client)
    apply(attStore, s3Client)
  }
}
