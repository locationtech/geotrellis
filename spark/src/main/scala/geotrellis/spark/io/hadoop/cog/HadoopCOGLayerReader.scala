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

package geotrellis.spark.io.hadoop.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.tiling.{Boundable, SpatialComponent}
import geotrellis.layers.{LayerId, TileLayerMetadata}
import geotrellis.layers._
import geotrellis.layers.cog.{ZoomRange, Extension}
import geotrellis.layers.hadoop.conf.HadoopConfig
import geotrellis.layers.hadoop._
import geotrellis.layers.hadoop.cog.byteReader
import geotrellis.layers.index.Index
import geotrellis.spark.io.cog._
import geotrellis.spark.io.hadoop._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import java.net.URI

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from HDFS.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class HadoopCOGLayerReader(
  val attributeStore: AttributeStore,
  val defaultThreads: Int = HadoopCOGLayerReader.defaultThreadCount
)(@transient implicit val sc: SparkContext) extends COGLayerReader[LayerId] with LazyLogging {

  val hadoopConfiguration = SerializableConfiguration(sc.hadoopConfiguration)

  val defaultNumPartitions: Int = sc.defaultParallelism

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, hadoopConfiguration.value)

  def pathExists(path: String): Boolean =
    HdfsUtils.pathExists(new Path(path), hadoopConfiguration.value)

  def fullPath(path: String): URI =
    new URI(path)

  def getHeader(id: LayerId): HadoopLayerHeader =
    try {
      attributeStore.readHeader[HadoopLayerHeader](LayerId(id.name, 0))
    } catch {
      case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
    }

  def produceGetKeyPath(id: LayerId): (ZoomRange, Int) => BigInt => String = {
    val header = getHeader(id)

    (zoomRange: ZoomRange, maxWidth: Int) =>
      (index: BigInt) =>
        s"${header.path}/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.$Extension"
  }

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], numPartitions: Int) =
    baseReadAllBands[K, V](
      id              = id,
      tileQuery       = tileQuery,
      numPartitions   = numPartitions,
      defaultThreads  = defaultThreads
    )

  def readSubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](
    id: LayerId,
    targetBands: Seq[Int],
    rasterQuery: LayerQuery[K, TileLayerMetadata[K]],
    numPartitions: Int
  ) =
    baseReadSubsetBands[K](id, targetBands, rasterQuery, numPartitions, defaultThreads)
}

object HadoopCOGLayerReader {
  val defaultThreadCount: Int = HadoopConfig.threads.rdd.readThreads

  def apply(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext): HadoopCOGLayerReader =
    new HadoopCOGLayerReader(attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGLayerReader =
    apply(HadoopAttributeStore(rootPath))
}
