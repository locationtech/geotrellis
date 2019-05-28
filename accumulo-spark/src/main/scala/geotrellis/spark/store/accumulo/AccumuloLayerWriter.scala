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

package geotrellis.spark.store.accumulo

import geotrellis.tiling.{Boundable, Bounds}
import geotrellis.layers._
import geotrellis.store.accumulo._
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.index._
import geotrellis.layers.merge.Mergable
import geotrellis.spark.store._
import geotrellis.spark.merge._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.spark.rdd.RDD

import spray.json._

import scala.reflect._

class AccumuloLayerWriter(
  val attributeStore: AttributeStore,
  instance: AccumuloInstance,
  table: String,
  options: AccumuloLayerWriter.Options
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
    validateUpdate[AccumuloLayerHeader, K, V, M](id, rdd.metadata) match {
      case Some(LayerAttributes(header, metadata, keyIndex, writerSchema)) =>

        val table = header.tileTable
        val encodeKey = (key: K) => AccumuloKeyEncoder.encode(id, key, keyIndex.toIndex(key))

        options.writeStrategy match {
          case _: HdfsWriteStrategy =>
            throw new IllegalArgumentException("HDFS Write strategy not supported in updates")
          case writeStrategy =>
            logger.info(s"Writing updated for layer ${id} to table $table")

            attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, writerSchema)
            AccumuloRDDWriter.update(
              rdd, instance, encodeKey, writeStrategy, table,
              Some(writerSchema), mergeFunc
            )
        }
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
      AccumuloLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        tileTable = table
      )
    val metadata = rdd.metadata
    val encodeKey = (key: K) => AccumuloKeyEncoder.encode(id, key, keyIndex.toIndex(key))

    // If no table exists, add the table and set the splits according to the
    // key index's keybounds and the number of partitions in the RDD.
    // This is a "best guess" scenario; users should use AccumuloUtils to
    // manually create splits based on their cluster configuration for best
    // performance.
    val ops = instance.connector.tableOperations()
    if (!ops.exists(table)) {
      ops.create(table)
      AccumuloUtils.addSplits(table, instance, keyIndex.keyBounds, keyIndex, rdd.partitions.length)
    }

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, schema)
      AccumuloRDDWriter.write(rdd, instance, encodeKey, options.writeStrategy, table)

      // Create locality groups based on encoding strategy
      for(lg <- AccumuloKeyEncoder.getLocalityGroups(id)) {
        instance.makeLocalityGroup(table, lg)
      }
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object AccumuloLayerWriter {
  case class Options(
    writeStrategy: AccumuloWriteStrategy = AccumuloWriteStrategy.DEFAULT
  )

  object Options {
    def DEFAULT = Options()

    implicit def writeStrategyToOptions(ws: AccumuloWriteStrategy): Options =
      Options(writeStrategy = ws)
  }

  def apply(
    instance: AccumuloInstance,
    table: String,
    options: Options
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      instance = instance,
      table = table,
      options = options
    )

  def apply(
    instance: AccumuloInstance,
    table: String
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      instance = instance,
      table = table,
      options = Options.DEFAULT
    )

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String,
    options: Options
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = attributeStore,
      instance = instance,
      table = table,
      options = options
    )

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String
  ): AccumuloLayerWriter =
    new AccumuloLayerWriter(
      attributeStore = attributeStore,
      instance = instance,
      table = table,
      options = Options.DEFAULT
    )
}
