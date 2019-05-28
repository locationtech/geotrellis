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

import geotrellis.layers.{ContextCollection, LayerId}
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json._

import scala.reflect._

class HBaseCollectionLayerReader(val attributeStore: AttributeStore, instance: HBaseInstance)
  extends CollectionLayerReader[LayerId] {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HBaseLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val seq = HBaseCollectionReader.read[K, V](instance, header.tileTable, id, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextCollection(seq, layerMetadata)
  }
}

object HBaseCollectionLayerReader {
  def apply(instance: HBaseInstance): HBaseCollectionLayerReader =
    new HBaseCollectionLayerReader(HBaseAttributeStore(instance), instance)

  def apply(attributeStore: HBaseAttributeStore): HBaseCollectionLayerReader =
    new HBaseCollectionLayerReader(attributeStore, attributeStore.instance)
}
