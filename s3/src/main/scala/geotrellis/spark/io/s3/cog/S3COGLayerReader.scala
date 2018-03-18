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
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import java.net.URI

import com.typesafe.config.ConfigFactory

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class S3COGLayerReader(
  val attributeStore: AttributeStore,
  val getS3Client: () => S3Client = () => S3Client.DEFAULT,
  val defaultThreads: Int = S3COGLayerReader.defaultThreadCount
)(@transient implicit val sc: SparkContext) extends COGLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions: Int = sc.defaultParallelism

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, getS3Client())

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], numPartitions: Int) = {
    val header =
      try {
        attributeStore.read[S3LayerHeader](LayerId(id.name, 0), COGAttributeStore.Fields.header)
      } catch {
        // to follow GeoTrellis Layer Readers logic
        case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
      }

    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      (index: BigInt) =>
        s"${header.bucket}/${header.key}/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.${Extension}"

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      numPartitions   = numPartitions,
      getKeyPath      = getKeyPath,
      pathExists      = { s3PathExists(_, getS3Client()) },
      fullPath        = { path => new URI(s"s3://$path") },
      defaultThreads  = defaultThreads
    )
  }
}

object S3COGLayerReader {
  lazy val defaultThreadCount: Int = ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.read")

  def apply(attributeStore: S3AttributeStore)(implicit sc: SparkContext): S3COGLayerReader =
    new S3COGLayerReader(
      attributeStore,
      () => attributeStore.s3Client
    )
}
