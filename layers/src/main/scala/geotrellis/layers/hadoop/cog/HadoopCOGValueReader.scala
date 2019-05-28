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

package geotrellis.layers.hadoop.cog

import geotrellis.tiling.SpatialComponent
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layers.LayerId
import geotrellis.layers._
import geotrellis.layers.cog.{COGReader, OverzoomingCOGValueReader, ZoomRange, Extension}
import geotrellis.layers.hadoop.{HadoopAttributeStore, HadoopLayerHeader}
import geotrellis.layers.index.{Index, KeyIndex}
import geotrellis.util._

import spray.json._

import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration

import java.net.URI

import scala.reflect.ClassTag


class HadoopCOGValueReader(
  val attributeStore: AttributeStore,
  conf: Configuration
) extends OverzoomingCOGValueReader {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)

  def reader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader
  ](layerId: LayerId): COGReader[K, V] = {

    val header =
      try {
        attributeStore.readHeader[HadoopLayerHeader](LayerId(layerId.name, 0))
      } catch {
        case e: AttributeNotFoundError => throw new LayerNotFoundError(layerId).initCause(e)
      }

    def keyPath(key: K, maxWidth: Int, baseKeyIndex: KeyIndex[K], zoomRange: ZoomRange): String =
      s"${header.path}/${layerId.name}/" +
      s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
      s"${Index.encode(baseKeyIndex.toIndex(key), maxWidth)}.$Extension"

    baseReader[K, V](
      layerId,
      keyPath,
      path => new URI(path),
      key => { case e: java.io.FileNotFoundException => throw new ValueNotFoundError(key, layerId) }
    )
  }
}

object HadoopCOGValueReader {
  def apply(attributeStore: HadoopAttributeStore): HadoopCOGValueReader =
    new HadoopCOGValueReader(attributeStore, attributeStore.conf)

  def apply(rootPath: Path, conf: Configuration): HadoopCOGValueReader =
    apply(HadoopAttributeStore(rootPath, conf))
}
