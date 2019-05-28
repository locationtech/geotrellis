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

package geotrellis.spark.store.cassandra

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.store.cassandra._
import geotrellis.layers.index._
import geotrellis.layers.merge.Mergable
import geotrellis.spark.store._
import geotrellis.spark.merge._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.spark.rdd.RDD

import spray.json._

import scala.reflect._

class CassandraLayerWriter(
  val attributeStore: AttributeStore,
  instance: CassandraInstance,
  keyspace: String,
  table: String
) extends LayerWriter[LayerId] with LazyLogging {

  // Layer updating
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
    validateUpdate[CassandraLayerHeader, K, V, M](id, rdd.metadata) match {
      case Some(LayerAttributes(header, metadata, keyIndex, writerSchema)) =>
        val (keyspace, table) = header.keyspace -> header.tileTable

        logger.info(s"Writing update for layer ${id} to table $table")

        val encodeKey = (key: K) => keyIndex.toIndex(key)
        attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, writerSchema)
        CassandraRDDWriter.update(rdd, instance, id, encodeKey, keyspace, table, Some(writerSchema), mergeFunc)

      case None =>
        logger.warn(s"Skipping update with empty bounds for $id.")
    }
  }

  // Layer writing
  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    val header =
      CassandraLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        keyspace = keyspace,
        tileTable = table
      )
    val metadata = rdd.metadata
    val encodeKey = (key: K) => keyIndex.toIndex(key)

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, schema)
      CassandraRDDWriter.write(rdd, instance, id, encodeKey, keyspace, table)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object CassandraLayerWriter {
  def apply(
    instance: CassandraInstance,
    keyspace: String,
    table: String
  ): CassandraLayerWriter =
    new CassandraLayerWriter(
      attributeStore = CassandraAttributeStore(instance),
      instance = instance,
      keyspace = keyspace,
      table = table
    )

  def apply(
    attributeStore: CassandraAttributeStore,
    keyspace: String,
    table: String
  ): CassandraLayerWriter =
    new CassandraLayerWriter(
      attributeStore = attributeStore,
      instance = attributeStore.instance,
      keyspace = keyspace,
      table = table
    )
}
