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
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index.Index
import geotrellis.spark.io.hadoop.conf.HadoopConfig
import geotrellis.spark.io.hadoop._
import geotrellis.util._

import spray.json.JsonFormat
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

import java.net.URI

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from HDFS.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class HadoopCOGCollectionLayerReader(
  val attributeStore: AttributeStore,
  val catalogPath: String,
  val conf: SerializableConfiguration = SerializableConfiguration(new Configuration),
  val defaultThreads: Int = HadoopCOGCollectionLayerReader.defaultThreadCount
)
  extends COGCollectionLayerReader[LayerId] with LazyLogging {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, conf.value)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: GeoTiffReader: ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]]) = {
    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      (index: BigInt) =>
        s"${catalogPath.toString}/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.$Extension"

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      getKeyPath      = getKeyPath,
      pathExists      = { str => HdfsUtils.pathExists(new Path(str), conf.value) },
      fullPath        = { path => new URI(path) },
      defaultThreads  = defaultThreads
    )
  }
}

object HadoopCOGCollectionLayerReader {
  val defaultThreadCount: Int = HadoopConfig.threads.collection.readThreads

  def apply(attributeStore: HadoopAttributeStore): HadoopCOGCollectionLayerReader =
    new HadoopCOGCollectionLayerReader(attributeStore, attributeStore.rootPath.toString, attributeStore.conf)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGCollectionLayerReader =
    apply(HadoopAttributeStore(rootPath))

  def apply(rootPath: Path, conf: Configuration): HadoopCOGCollectionLayerReader =
    apply(HadoopAttributeStore(rootPath, conf))
}

