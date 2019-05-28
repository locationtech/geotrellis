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

package geotrellis.spark.io.accumulo

import geotrellis.tiling.{Boundable, Bounds, KeyBounds}
import geotrellis.layers._
import geotrellis.layers.AttributeStore.Fields
import geotrellis.layers.accumulo._
import geotrellis.layers.avro._
import geotrellis.layers.index._
import geotrellis.layers.json._
import geotrellis.util._

import org.apache.avro._
import org.apache.spark.SparkContext

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class AccumuloLayerCopier(
  attributeStore: AttributeStore,
  layerReader: AccumuloLayerReader,
  getLayerWriter: LayerId => AccumuloLayerWriter
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

object AccumuloLayerCopier {
  def apply(
    attributeStore: AttributeStore,
    layerReader: AccumuloLayerReader,
    getLayerWriter: LayerId => AccumuloLayerWriter
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    new AccumuloLayerCopier(
      attributeStore,
      layerReader,
      getLayerWriter
    )

  def apply(
    attributeStore: AttributeStore,
    layerReader: AccumuloLayerReader,
    layerWriter: AccumuloLayerWriter
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      attributeStore,
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance   : AccumuloInstance,
    layerReader: AccumuloLayerReader,
    layerWriter: AccumuloLayerWriter
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      AccumuloAttributeStore(instance.connector),
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance: AccumuloInstance,
    targetTable: String,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      AccumuloAttributeStore(instance),
      AccumuloLayerReader(instance),
      _ => AccumuloLayerWriter(instance, targetTable, options)
    )

  def apply(
   instance: AccumuloInstance,
   targetTable: String
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      instance,
      targetTable,
      AccumuloLayerWriter.Options.DEFAULT
    )

  def apply(
    instance: AccumuloInstance,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerCopier = {
    val attributeStore = AccumuloAttributeStore(instance)
    apply(
      attributeStore,
      AccumuloLayerReader(instance),
      { layerId: LayerId =>
        val header = attributeStore.readHeader[AccumuloLayerHeader](layerId)
        AccumuloLayerWriter(instance, header.tileTable, options)
      }
    )
  }

  def apply(
   instance: AccumuloInstance
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      instance,
      AccumuloLayerWriter.Options.DEFAULT
    )
}
