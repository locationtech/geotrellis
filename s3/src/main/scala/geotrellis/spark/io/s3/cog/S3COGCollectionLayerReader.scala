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

package geotrellis.spark.io.s3.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.tiling.{Boundable, SpatialComponent}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.layers.io.index._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.conf.S3Config
import geotrellis.util._
import com.typesafe.scalalogging.LazyLogging
import spray.json.JsonFormat
import java.net.URI

import geotrellis.layers.io.cog.{COGCollectionLayerReader, ZoomRange}
import geotrellis.layers.{LayerId, TileLayerMetadata}

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class S3COGCollectionLayerReader(
  val attributeStore: AttributeStore,
  val getS3Client: () => S3Client = () => S3Client.DEFAULT,
  val defaultThreads: Int = S3COGCollectionLayerReader.defaultThreadCount
) extends COGCollectionLayerReader[LayerId] with LazyLogging {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, getS3Client())

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: LayerId, rasterQuery: LayerQuery[K, TileLayerMetadata[K]]) = {
    val header =
      try {
        attributeStore.readHeader[S3LayerHeader](LayerId(id.name, 0))
      } catch {
        // to follow GeoTrellis Layer Readers logic
        case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
      }

    val bucket = header.bucket
    val prefix = header.key

    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      (index: BigInt) =>
        s"$bucket/$prefix/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.${Extension}"

    baseRead[K, V](
      id              = id,
      tileQuery       = rasterQuery,
      getKeyPath      = getKeyPath,
      pathExists      = { s3PathExists(_, getS3Client()) },
      fullPath        = { path => new URI(s"s3://$path") },
      defaultThreads  = defaultThreads
    )
  }
}

object S3COGCollectionLayerReader {
  lazy val defaultThreadCount: Int = S3Config.threads.collection.readThreads

  def apply(attributeStore: S3AttributeStore): S3COGCollectionLayerReader =
    new S3COGCollectionLayerReader(
      attributeStore,
      () => attributeStore.s3Client
    )

  def apply(bucket: String, prefix: String): S3COGCollectionLayerReader =
    apply(S3AttributeStore(bucket, prefix))
}
