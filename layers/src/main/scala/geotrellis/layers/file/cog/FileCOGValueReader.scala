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

package geotrellis.layers.file.cog


import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.layers.LayerId
import geotrellis.layers._
import geotrellis.layers.cog._
import geotrellis.layers.file.{FileAttributeStore, KeyPathGenerator}
import geotrellis.layers.index._
import geotrellis.util._

import spray.json.JsonFormat

import java.net.URI

import scala.reflect.ClassTag

class FileCOGValueReader(
  val attributeStore: AttributeStore,
  catalogPath: String
) extends OverzoomingCOGValueReader {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)

  def reader[
    K: JsonFormat : SpatialComponent : ClassTag,
    V <: CellGrid[Int] : GeoTiffReader
  ](layerId: LayerId): COGReader[K, V] = {
    def keyPath(key: K, maxWidth: Int, baseKeyIndex: KeyIndex[K], zoomRange: ZoomRange): String =
      (KeyPathGenerator(catalogPath, s"${layerId.name}/${zoomRange.slug}", baseKeyIndex, maxWidth) andThen (_ ++ s".$Extension"))(key)

    baseReader[K, V](
      layerId,
      keyPath,
      new URI(_),
      key => { case e: java.io.FileNotFoundException => throw new ValueNotFoundError(key, layerId) }
    )
  }
}

object FileCOGValueReader {
  def apply[
    K: JsonFormat : SpatialComponent : ClassTag,
    V <: CellGrid[Int] : GeoTiffReader
  ](attributeStore: AttributeStore, catalogPath: String, layerId: LayerId): Reader[K, V] =
    new FileCOGValueReader(attributeStore, catalogPath).reader(layerId)

  def apply(catalogPath: String): FileCOGValueReader =
    new FileCOGValueReader(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore): FileCOGValueReader =
    new FileCOGValueReader(attributeStore, attributeStore.catalogPath)
}
