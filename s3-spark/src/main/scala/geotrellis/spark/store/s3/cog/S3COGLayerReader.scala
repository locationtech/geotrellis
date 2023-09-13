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

package geotrellis.spark.store.s3.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.util._
import geotrellis.store.cog._
import geotrellis.store.index._
import geotrellis.store.s3._
import geotrellis.spark.store.cog._

import org.apache.spark.SparkContext
import software.amazon.awssdk.services.s3._
import software.amazon.awssdk.services.s3.model._
import io.circe._
import cats.effect._

import scala.reflect.ClassTag
import java.net.URI

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class S3COGLayerReader(
  val attributeStore: AttributeStore,
  s3Client: => S3Client = S3ClientProducer.get(),
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
)(@transient implicit val sc: SparkContext) extends COGLayerReader[LayerId] {
  @transient implicit lazy val ioRuntime: unsafe.IORuntime = runtime

  val defaultNumPartitions: Int = sc.defaultParallelism

  def pathExists(path: String): Boolean = s3Client.objectExists(path)

  def fullPath(path: String): URI = new URI(s"s3://$path")

  def getHeader(id: LayerId): S3LayerHeader =
    try {
      attributeStore.readHeader[S3LayerHeader](LayerId(id.name, 0))
    } catch {
      // to follow GeoTrellis Layer Readers logic
      case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
      case e: NoSuchBucketException => throw new LayerNotFoundError(id).initCause(e)
    }

  def produceGetKeyPath(id: LayerId): (ZoomRange, Int) => BigInt => String = {
    val header = getHeader(id)

    (zoomRange: ZoomRange, maxWidth: Int) =>
      (index: BigInt) =>
        s"${header.bucket}/${header.key}/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.${Extension}"
  }

  def read[
    K: SpatialComponent: Boundable: Decoder: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], numPartitions: Int) =
    baseReadAllBands[K, V](
      id              = id,
      tileQuery       = tileQuery,
      numPartitions   = numPartitions
    )

  def readSubsetBands[
    K: SpatialComponent: Boundable: Decoder: ClassTag
  ](
    id: LayerId,
    targetBands: Seq[Int],
    rasterQuery: LayerQuery[K, TileLayerMetadata[K]],
    numPartitions: Int
  ) =
    baseReadSubsetBands[K](id, targetBands, rasterQuery, numPartitions)
}

object S3COGLayerReader {
  def apply(attributeStore: S3AttributeStore)(implicit sc: SparkContext): S3COGLayerReader =
    new S3COGLayerReader(
      attributeStore,
      attributeStore.client,
      IORuntimeTransient.IORuntime
    )
}
