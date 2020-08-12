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

package geotrellis.store.hadoop

import geotrellis.layer.{Boundable, Bounds}
import geotrellis.store._
import geotrellis.store.avro._
import geotrellis.store.hadoop.util._
import geotrellis.util._

import io.circe._
import scala.reflect.ClassTag

import org.apache.hadoop.fs.Path

import scala.reflect.ClassTag

class HadoopLayerCopier(
   rootPath: Path,
   val attributeStore: HadoopAttributeStore
) extends LayerCopier[LayerId] {
  def copy[
    K: AvroRecordCodec: Boundable: Encoder: Decoder: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Encoder: Decoder: Component[*, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(from).initCause(e)
    }
    val newPath = new Path(rootPath,  s"${to.name}/${to.zoom}")
    HdfsUtils.copyPath(new Path(header.path), newPath, attributeStore.conf)
    attributeStore.writeLayerAttributes(
      to, header.copy(path = newPath.toUri), metadata, keyIndex, writerSchema
    )
  }
}

object HadoopLayerCopier {
  def apply(
    rootPath: Path,
    attributeStore: HadoopAttributeStore
  ): HadoopLayerCopier =
    new HadoopLayerCopier(rootPath, attributeStore)

  def apply(attributeStore: HadoopAttributeStore): HadoopLayerCopier =
    apply(attributeStore.rootPath, attributeStore)
}
