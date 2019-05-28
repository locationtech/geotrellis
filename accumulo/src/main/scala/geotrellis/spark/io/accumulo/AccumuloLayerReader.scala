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

import geotrellis.tiling.{Boundable, Bounds, EmptyBounds, KeyBounds}
import geotrellis.layers._
import geotrellis.layers.accumulo._
import geotrellis.layers.avro._
import geotrellis.spark.ContextRDD
import geotrellis.spark.io.FilteringLayerReader
import geotrellis.util._

import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.accumulo.core.data.{Range => AccumuloRange}

import spray.json._

import scala.reflect._

class AccumuloLayerReader(val attributeStore: AttributeStore)(implicit sc: SparkContext, instance: AccumuloInstance)
    extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, tileQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = tileQuery(metadata)
    val layerBounds = metadata.getComponent[Bounds[K]]
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))

    val decompose: KeyBounds[K] => Seq[AccumuloRange] =
      if(queryKeyBounds.size == 1 && queryKeyBounds.head.contains(layerBounds)) {
        // This query is asking for all the keys of the layer;
        // avoid a heavy set of accumulo ranges by not setting any at all,
        // which equates to a full request.
        { _ => Seq(new AccumuloRange()) }
      } else {
        (bounds: KeyBounds[K]) => {
          keyIndex.indexRanges(bounds).map { case (min, max) =>
            new AccumuloRange(new Text(AccumuloKeyEncoder.long2Bytes(min)), new Text(AccumuloKeyEncoder.long2Bytes(max)))
          }
        }
      }

    val rdd = AccumuloRDDReader.read[K, V](header.tileTable, columnFamily(id), queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextRDD(rdd, layerMetadata)
  }
}

object AccumuloLayerReader {
  def apply(instance: AccumuloInstance)(implicit sc: SparkContext): AccumuloLayerReader =
    new AccumuloLayerReader(AccumuloAttributeStore(instance.connector))(sc, instance)

  def apply(attributeStore: AccumuloAttributeStore)(implicit sc: SparkContext, instance: AccumuloInstance): AccumuloLayerReader =
    new AccumuloLayerReader(attributeStore)

  def apply()(implicit sc: SparkContext, instance: AccumuloInstance): AccumuloLayerReader =
    new AccumuloLayerReader(AccumuloAttributeStore(instance.connector))
}
