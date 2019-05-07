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

package geotrellis.spark.io.s3.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.tiling.SpatialComponent
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.index.{Index, KeyIndex}
import geotrellis.spark.io.s3.{S3AttributeStore, S3Client, S3LayerHeader, S3RDDWriter, makePath}
import geotrellis.spark.io.cog._
import geotrellis.spark.io.cog.vrt.VRT.IndexedSimpleSource
import spray.json.JsonFormat
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata, PutObjectRequest}
import java.io.ByteArrayInputStream

import geotrellis.layers.LayerId
import geotrellis.layers.io.cog.{COGLayerStorageMetadata, ZoomRange}
import geotrellis.layers.io.cog.vrt.VRT

import scala.util.Try
import scala.reflect.{ClassTag, classTag}

class S3COGLayerWriter(
  val attributeStore: AttributeStore,
  bucket: String,
  keyPrefix: String,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  threads: Int = S3RDDWriter.defaultThreadCount
) extends COGLayerWriter {

  def writeCOGLayer[
    K: SpatialComponent: Ordering: JsonFormat: ClassTag,
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

    val s3Client = getS3Client() // for saving VRT from Accumulator

    // Make S3COGAsyncWriter
    val asyncWriter = new S3COGAsyncWriter[V](bucket, 32, p => p)

    val retryCheck: Throwable => Boolean = {
      case e: AmazonS3Exception if e.getStatusCode == 503 => true
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
        .foreachPartition { partition => asyncWriter.write(getS3Client(), partition, mergeFunc, Some(retryCheck)) }

      // Save Accumulator
      val bytes =
        vrt
          .fromAccumulator(samplesAccumulator)
          .outputStream
          .toByteArray

      val objectMetadata = new ObjectMetadata()
      objectMetadata.setContentLength(bytes.length.toLong)

      val request = new PutObjectRequest(
        bucket, makePath(prefix, "vrt.xml"),
        new ByteArrayInputStream(bytes),
        objectMetadata
      )
      s3Client.putObject(request)
      samplesAccumulator.reset
    }
  }
}

object S3COGLayerWriter {
  def apply(attributeStore: S3AttributeStore): S3COGLayerWriter =
    new S3COGLayerWriter(attributeStore, attributeStore.bucket, attributeStore.prefix, () => attributeStore.s3Client)
}


class S3COGAsyncWriter[V <: CellGrid[Int]: GeoTiffReader](
  bucket: String,
  threads: Int,
  putObjectModifier: PutObjectRequest => PutObjectRequest
) extends  AsyncWriter[S3Client, GeoTiff[V], PutObjectRequest](threads) {

  def readRecord(
    client: S3Client,
    key: String
  ): Try[GeoTiff[V]] = Try {
    val is = client.getObject(bucket, key).getObjectContent
    val bytes = sun.misc.IOUtils.readFully(is, Int.MaxValue, true)
    GeoTiffReader[V].read(bytes)
  }

  def encodeRecord(key: String, value: GeoTiff[V]): PutObjectRequest = {
    val bytes: Array[Byte] = GeoTiffWriter.write(value, true)
    val is = new ByteArrayInputStream(bytes)

    val metadata = new ObjectMetadata()
    metadata.setContentLength(bytes.length.toLong)
    putObjectModifier(new PutObjectRequest(bucket, key, is, metadata))
  }

  def writeRecord(
    client: S3Client,
    key: String,
    encoded: PutObjectRequest
  ): Try[Long] = Try {
    encoded.getInputStream.reset() // required if this is a retry
    client.putObject(encoded)
    encoded.getMetadata.getContentLength()
  }
}
