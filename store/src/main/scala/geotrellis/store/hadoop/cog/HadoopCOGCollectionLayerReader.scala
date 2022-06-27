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

package geotrellis.store.hadoop.cog

import geotrellis.layer._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.store._
import geotrellis.store.util._
import geotrellis.store.cog.{COGCollectionLayerReader, Extension, ZoomRange}
import geotrellis.store.hadoop.{HadoopAttributeStore, SerializableConfiguration}
import geotrellis.store.hadoop.util._
import geotrellis.store.index.Index

import cats.effect._
import _root_.io.circe._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import scala.reflect.ClassTag
import java.net.URI

/**
 * Handles reading raster RDDs and their metadata from HDFS.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class HadoopCOGCollectionLayerReader(
  val attributeStore: AttributeStore,
  val catalogPath: String,
  val conf: Configuration = new Configuration,
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
) extends COGCollectionLayerReader[LayerId] {

  val serConf: SerializableConfiguration = SerializableConfiguration(conf)

  @transient implicit lazy val ioRuntime: unsafe.IORuntime = runtime

  def read[
    K: SpatialComponent: Boundable: Decoder: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]]) = {
    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      (index: BigInt) =>
        s"${catalogPath}/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.$Extension"

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      getKeyPath      = getKeyPath,
      pathExists      = { str => HdfsUtils.pathExists(new Path(str), conf) },
      fullPath        = { path => new URI(path) }
    )
  }
}

object HadoopCOGCollectionLayerReader {
  def apply(attributeStore: HadoopAttributeStore): HadoopCOGCollectionLayerReader =
    new HadoopCOGCollectionLayerReader(attributeStore, attributeStore.rootPath.toString, attributeStore.conf)

  def apply(rootPath: Path, conf: Configuration): HadoopCOGCollectionLayerReader =
    apply(HadoopAttributeStore(rootPath, conf))
}
