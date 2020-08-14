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

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layer._
import geotrellis.store._
import geotrellis.store.cog.{COGReader, OverzoomingCOGValueReader, ZoomRange, Extension}
import geotrellis.store.hadoop.{HadoopAttributeStore, HadoopLayerHeader}
import geotrellis.store.index.{Index, KeyIndex}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import _root_.io.circe._

import java.net.URI
import scala.reflect.ClassTag

class HadoopCOGValueReader(
  val attributeStore: AttributeStore,
  conf: Configuration
) extends OverzoomingCOGValueReader {

  def reader[
    K: Decoder: SpatialComponent: ClassTag,
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
