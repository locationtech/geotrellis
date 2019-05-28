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

package geotrellis.spark.io.hbase

import geotrellis.layers.LayerId
import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HBaseLayerCopier(
  attributeStore: AttributeStore,
  layerReader: HBaseLayerReader,
  getLayerWriter: LayerId => HBaseLayerWriter
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
      getLayerWriter(from).write(to, layerReader.read[K, V, M](from), keyIndex)
    } catch {
      case e: Exception => throw new LayerCopyError(from, to).initCause(e)
    }
  }
}

object HBaseLayerCopier {
  def apply(
    attributeStore: AttributeStore,
    layerReader: HBaseLayerReader,
    getLayerWriter: LayerId => HBaseLayerWriter
  )(implicit sc: SparkContext): HBaseLayerCopier =
    new HBaseLayerCopier(
      attributeStore,
      layerReader,
      getLayerWriter
    )

  def apply(
    attributeStore: AttributeStore,
    layerReader: HBaseLayerReader,
    layerWriter: HBaseLayerWriter
  )(implicit sc: SparkContext): HBaseLayerCopier =
    apply(
      attributeStore,
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance   : HBaseInstance,
    layerReader: HBaseLayerReader,
    layerWriter: HBaseLayerWriter
  )(implicit sc: SparkContext): HBaseLayerCopier =
    apply(
      HBaseAttributeStore(instance),
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance: HBaseInstance,
    targetTable: String
  )(implicit sc: SparkContext): HBaseLayerCopier =
    apply(
      HBaseAttributeStore(instance),
      HBaseLayerReader(instance),
      _ => HBaseLayerWriter(instance, targetTable)
    )

  def apply(
    instance: HBaseInstance
  )(implicit sc: SparkContext): HBaseLayerCopier = {
    val attributeStore = HBaseAttributeStore(instance)
    apply(
      attributeStore,
      HBaseLayerReader(instance),
      { layerId: LayerId =>
        val header = attributeStore.readHeader[HBaseLayerHeader](layerId)
        HBaseLayerWriter(instance, header.tileTable)
      }
    )
  }
}
