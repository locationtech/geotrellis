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

import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.avro.AvroRecordCodec
import geotrellis.util._

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.services.s3.S3Client
import _root_.io.circe._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

class S3LayerCopier(
  val attributeStore: AttributeStore,
  destBucket: String,
  destKeyPrefix: String,
  s3Client: => S3Client = S3ClientProducer.get()
) extends LayerCopier[LayerId] {

  // Not necessary if this isn't recursive any longer due to the iterator handling *all* objects
  // @tailrec
  final def copyListing(s3Client: S3Client, bucket: String, listing: Iterable[S3Object], from: LayerId, to: LayerId): Unit = {
    listing.foreach { s3obj =>
      val request =
        CopyObjectRequest.builder()
          .sourceBucket(bucket)
          .sourceKey(s3obj.key)
          .destinationBucket(bucket)
          .destinationKey(s3obj.key.replace(s"${from.name}/${from.zoom}", s"${to.name}/${to.zoom}"))
          .build()

      s3Client.copyObject(request)
    }
    // Appears to no longer be necessary; TODO: remove
    // if (listing.isTruncated) copyListing(s3Client, bucket, s3Client.listNextBatchOfObjects(listing), from, to)
  }

  def copy[
    K: AvroRecordCodec: Boundable: Encoder: Decoder: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Encoder: Decoder: Component[*, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val LayerAttributes(header, metadata, keyIndex, schema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(from).initCause(e)
      case e: NoSuchBucketException => throw new LayerReadError(from).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key

    val listRequest =
      ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(prefix)
        .build()
    val objIter =
      s3Client
        .listObjectsV2Paginator(listRequest)
        .contents
        .asScala
    copyListing(s3Client, bucket, objIter, from, to)
    attributeStore.copy(from, to)
    attributeStore.writeLayerAttributes(
      to, header.copy(
        bucket = destBucket,
        key    = makePath(destKeyPrefix, s"${to.name}/${to.zoom}")
      ), metadata, keyIndex, schema
    )
  }
}

object S3LayerCopier {
  def apply(attributeStore: AttributeStore, destBucket: String, destKeyPrefix: String, s3Client: => S3Client): S3LayerCopier =
    new S3LayerCopier(attributeStore, destBucket, destKeyPrefix, s3Client)

  def apply(bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String, s3Client: => S3Client): S3LayerCopier = {
    val attStore = S3AttributeStore(bucket, keyPrefix, s3Client)
    apply(attStore, destBucket, destKeyPrefix, s3Client)
  }

  def apply(bucket: String, keyPrefix: String, s3Client: => S3Client): S3LayerCopier = {
    val attStore = S3AttributeStore(bucket, keyPrefix, s3Client)
    apply(attStore)
  }

  def apply(attributeStore: S3AttributeStore): S3LayerCopier =
    apply(attributeStore, attributeStore.bucket, attributeStore.prefix, attributeStore.client)
}
