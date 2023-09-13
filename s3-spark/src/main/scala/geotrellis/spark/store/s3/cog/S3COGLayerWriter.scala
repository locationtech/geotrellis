/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.store.s3.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.cog._
import geotrellis.store.cog.vrt.VRT
import geotrellis.store.cog.vrt.VRT.IndexedSimpleSource
import geotrellis.store.index.{Index, KeyIndex}
import geotrellis.store.s3._
import geotrellis.spark.store.cog._
import geotrellis.store.util.IORuntimeTransient

import software.amazon.awssdk.services.s3.model.{GetObjectRequest, PutObjectRequest, S3Exception}
import software.amazon.awssdk.services.s3._
import software.amazon.awssdk.core.sync.RequestBody
import org.apache.commons.io.IOUtils
import _root_.io.circe._
import cats.effect._

import scala.util.Try
import scala.reflect.{ClassTag, classTag}

class S3COGLayerWriter(
  val attributeStore: AttributeStore,
  bucket: String,
  keyPrefix: String,
  s3Client: => S3Client = S3ClientProducer.get(),
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
) extends COGLayerWriter {

  def writeCOGLayer[
    K: SpatialComponent: Ordering: Encoder: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](
    layerName: String,
    cogLayer: COGLayer[K, V],
    keyIndexes: Map[ZoomRange, KeyIndex[K]],
    mergeFunc: Option[(GeoTiff[V], GeoTiff[V]) => GeoTiff[V]] = None
  ): Unit = {
    /** Collect VRT into accumulators, to write everything and to collect VRT at the same time */
    val layerId0 = LayerId(layerName, 0)
    val sc = cogLayer.layers.head._2.sparkContext
    val samplesAccumulator = sc.collectionAccumulator[IndexedSimpleSource](VRT.accumulatorName(layerName))
    val storageMetadata = COGLayerStorageMetadata(cogLayer.metadata, keyIndexes)

    val header = S3LayerHeader(
      keyClass = classTag[K].toString(),
      valueClass = classTag[V].toString(),
      bucket = bucket,
      key = keyPrefix,
      layerType = COGLayerType
    )

    attributeStore.writeCOGLayerAttributes(layerId0, header, storageMetadata)

    // Make S3COGAsyncWriter
    val asyncWriter = new S3COGAsyncWriter[V](bucket, p => p, runtime)

    val retryCheck: Throwable => Boolean = {
      case e: S3Exception if e.statusCode == 503 => true
      case _ => false
    }

    for((zoomRange, cogs) <- cogLayer.layers.toSeq.sortBy(_._1)(Ordering[ZoomRange].reverse)) {
      val vrt = VRT(cogLayer.metadata.tileLayerMetadata(zoomRange.minZoom))

      // Make RDD[(String, GeoTiff[T])]
      val keyIndex = keyIndexes(zoomRange)
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val prefix   = makePath(keyPrefix, s"${layerName}/${zoomRange.minZoom}_${zoomRange.maxZoom}")
      val keyPath  = (key: K) => makePath(prefix, Index.encode(keyIndex.toIndex(key), maxWidth))

      // Save all partitions
      cogs
        .map { case (key, cog) =>
          // collect VRT metadata
          (0 until cog.bandCount).foreach { b =>
            val idx = Index.encode(keyIndex.toIndex(key), maxWidth)
            val simpleSource = vrt.simpleSource(s"$idx.$Extension", b + 1, cog.cols, cog.rows, cog.extent)
            samplesAccumulator.add((idx.toLong, simpleSource))
          }

          (s"${keyPath(key)}.${Extension}", cog)
        }
        .foreachPartition { partition => asyncWriter.write(s3Client, partition, mergeFunc, Some(retryCheck)) }

      // Save Accumulator
      val bytes =
        vrt
          .fromAccumulator(samplesAccumulator)
          .outputStream
          .toByteArray

      val request =
        PutObjectRequest.builder()
          .bucket(bucket)
          .key(makePath(prefix, "vrt.xml"))
          .contentLength(bytes.length)
          .build()

      val requestBody = RequestBody.fromBytes(bytes)

      s3Client.putObject(request, requestBody)
      samplesAccumulator.reset()
    }
  }
}

object S3COGLayerWriter {
  def apply(attributeStore: S3AttributeStore): S3COGLayerWriter =
    new S3COGLayerWriter(attributeStore, attributeStore.bucket, attributeStore.prefix, attributeStore.client)
}


class S3COGAsyncWriter[V <: CellGrid[Int]: GeoTiffReader](
  bucket: String,
  putObjectModifier: PutObjectRequest => PutObjectRequest,
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
) extends AsyncWriter[S3Client, GeoTiff[V], (PutObjectRequest, RequestBody)](runtime) {

  def readRecord(
    client: S3Client,
    key: String
  ): Try[GeoTiff[V]] = Try {
    val request = GetObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()
    val is = client.getObject(request)
    val bytes = IOUtils.toByteArray(is)
    is.close()
    GeoTiffReader[V].read(bytes)
  }

  def encodeRecord(key: String, value: GeoTiff[V]): (PutObjectRequest, RequestBody) = {
    val bytes: Array[Byte] = GeoTiffWriter.write(value, true)

    val request =
      PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentLength(bytes.length)
        .build()

    val requestBody =
      RequestBody.fromBytes(bytes)

    (putObjectModifier(request), requestBody)
  }

  def writeRecord(
    client: S3Client,
    key: String,
    encodedRequest: (PutObjectRequest, RequestBody)
  ): Try[Long] = Try {
    client.putObject(encodedRequest._1, encodedRequest._2)
    encodedRequest._1.contentLength
  }
}
