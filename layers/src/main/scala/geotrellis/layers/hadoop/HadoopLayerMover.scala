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

package geotrellis.layers.hadoop

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.layers.LayerId
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.util._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import spray.json.JsonFormat

import scala.reflect.ClassTag


class HadoopLayerMover(
  rootPath: Path,
  val attributeStore: HadoopAttributeStore
) extends LayerMover[LayerId] {
  override def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val conf = attributeStore.conf

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerMoveError(from, to).initCause(e)
    }
    val (newLayerRoot, newPath) = new Path(rootPath,  s"${to.name}") -> new Path(rootPath,  s"${to.name}/${to.zoom}")
    // new layer name root has to be created before layerId renaming
    HdfsUtils.ensurePathExists(newLayerRoot, conf)
    HdfsUtils.renamePath(new Path(header.path), newPath, conf)
    attributeStore.writeLayerAttributes(
      to, header.copy(path = newPath.toUri), metadata, keyIndex, writerSchema
    )
    attributeStore.delete(from)
    attributeStore.clearCache()
  }
}

object HadoopLayerMover {
  def apply(
    rootPath: Path,
    attributeStore: HadoopAttributeStore
  ): HadoopLayerMover =
    new HadoopLayerMover(rootPath, attributeStore)

  def apply(rootPath: Path): HadoopLayerMover =
    apply(rootPath, HadoopAttributeStore(rootPath, new Configuration))

  def apply(attributeStore: HadoopAttributeStore): HadoopLayerMover =
    apply(attributeStore.rootPath, attributeStore)
}
