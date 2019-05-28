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

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.layers.io.avro.codecs._
import geotrellis.layers.io.index._
import geotrellis.spark.merge._
import geotrellis.util._
import com.typesafe.scalalogging.LazyLogging
import geotrellis.layers.merge.Mergable
import geotrellis.layers.{LayerId, Metadata}
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class HBaseLayerWriter(
  val attributeStore: AttributeStore,
  instance: HBaseInstance,
  table: String
) extends LayerWriter[LayerId] with LazyLogging {

  // Layer Updating
  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M]
  ): Unit = {
    update(id, rdd, None)
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    mergeFunc: (V, V) => V
  ): Unit = {
    update(id, rdd, Some(mergeFunc))
  }

  private def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    mergeFunc: Option[(V, V) => V]
  ) = {
    validateUpdate[HBaseLayerHeader, K, V, M](id, rdd.metadata) match {
      case Some(LayerAttributes(header, metadata, keyIndex, writerSchema)) =>
        val table = header.tileTable
        logger.info(s"Writing update for layer ${id} to table $table")
        val encodeKey = (key: K) => keyIndex.toIndex(key)
        attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, writerSchema)
        HBaseRDDWriter.update(rdd, instance, id, encodeKey, table, Some(writerSchema), mergeFunc)

      case None =>
        logger.warn(s"Skipping update with empty bounds for $id.")
    }
  }

  // Layer Writing
  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    val header =
      HBaseLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        tileTable = table
      )
    val metadata = rdd.metadata
    val encodeKey = (key: K) => keyIndex.toIndex(key)

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, schema)
      HBaseRDDWriter.write(rdd, instance, id, encodeKey, table)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object HBaseLayerWriter {
  def apply(
    instance: HBaseInstance,
    table: String
  ): HBaseLayerWriter =
    new HBaseLayerWriter(
      attributeStore = HBaseAttributeStore(instance),
      instance = instance,
      table = table
    )

  def apply(
    attributeStore: HBaseAttributeStore,
    table: String
  ): HBaseLayerWriter =
    new HBaseLayerWriter(
      attributeStore = attributeStore,
      instance = attributeStore.instance,
      table = table
    )
}
