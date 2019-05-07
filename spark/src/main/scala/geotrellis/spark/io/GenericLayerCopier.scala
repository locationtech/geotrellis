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

package geotrellis.spark.io

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.layers.LayerId
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.index.KeyIndex
import geotrellis.util._
import org.apache.avro.Schema
import spray.json._

import scala.reflect._

class GenericLayerCopier[Header: JsonFormat](
  val attributeStore: AttributeStore,
  layerReader: LayerReader[LayerId],
  layerWriter: LayerWriter[LayerId]
) extends LayerCopier[LayerId] {

  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val keyIndex = try {
      attributeStore.readKeyIndex[K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      attributeStore.copy(from, to)
      layerWriter.write(to, layerReader.read[K, V, M](from), keyIndex)
    } catch {
      case e: Exception => throw new LayerCopyError(from, to).initCause(e)
    }
  }
}
