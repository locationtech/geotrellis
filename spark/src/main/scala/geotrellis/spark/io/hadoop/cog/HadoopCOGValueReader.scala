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
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index.{Index, KeyIndex}
import geotrellis.util._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json._

import java.net.URI

import scala.reflect.ClassTag

class HadoopCOGValueReader(
  val attributeStore: AttributeStore,
  conf: Configuration,
  catalogPath: Path,
  maxOpenFiles: Int = 16
) extends OverzoomingCOGValueReader {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)

  def reader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid: TiffMethods
  ](layerId: LayerId): Reader[K, V] = {
    def keyPath(key: K, maxWidth: Int, baseKeyIndex: KeyIndex[K], zoomRange: ZoomRange): String =
      s"${catalogPath.toString}/${layerId.name}/" +
      s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
      s"${Index.encode(baseKeyIndex.toIndex(key), maxWidth)}.$Extension"

    baseReader[K, V](
      layerId,
      keyPath,
      path => new URI(s"hdfs://${path}")
    )
  }
}
