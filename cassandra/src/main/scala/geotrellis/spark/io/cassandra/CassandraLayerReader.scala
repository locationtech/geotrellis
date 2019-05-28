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

package geotrellis.spark.io.cassandra

import geotrellis.layers.LayerId
import geotrellis.tiling.{Boundable, Bounds, EmptyBounds, KeyBounds}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json._

import scala.reflect._

class CassandraLayerReader(val attributeStore: AttributeStore, instance: CassandraInstance)(implicit sc: SparkContext)
  extends FilteringLayerReader[LayerId] {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
  K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag,
  M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, tileQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[CassandraLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = tileQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val rdd = CassandraRDDReader.read[K, V](instance, header.keyspace, header.tileTable, id, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextRDD(rdd, layerMetadata)
  }
}

object CassandraLayerReader {
  def apply(instance: CassandraInstance)(implicit sc: SparkContext): CassandraLayerReader =
    new CassandraLayerReader(CassandraAttributeStore(instance), instance)(sc)

  def apply(attributeStore: CassandraAttributeStore)(implicit sc: SparkContext): CassandraLayerReader =
    new CassandraLayerReader(attributeStore, attributeStore.instance)
}
