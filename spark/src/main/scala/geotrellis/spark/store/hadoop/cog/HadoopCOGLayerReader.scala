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

package geotrellis.spark.store.hadoop.cog

import io.circe._

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.cog.{Extension, ZoomRange}
import geotrellis.store.hadoop._
import geotrellis.store.hadoop.util._
import geotrellis.store.index.Index
import geotrellis.spark.store.cog._
import geotrellis.spark.store.hadoop._
import geotrellis.store.util.IORuntimeTransient

import cats.effect._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import java.net.URI

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from HDFS.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class HadoopCOGLayerReader(
  val attributeStore: AttributeStore,
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
)(@transient implicit val sc: SparkContext) extends COGLayerReader[LayerId] {

  @transient implicit lazy val ioRuntime: unsafe.IORuntime = runtime

  val hadoopConfiguration = SerializableConfiguration(sc.hadoopConfiguration)

  val defaultNumPartitions: Int = sc.defaultParallelism

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

object HadoopCOGLayerReader {
  def apply(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext): HadoopCOGLayerReader =
    new HadoopCOGLayerReader(attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGLayerReader =
    apply(HadoopAttributeStore(rootPath))
}
