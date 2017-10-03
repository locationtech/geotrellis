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

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.merge._
import geotrellis.util._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import spray.json._

import scala.reflect._

class AccumuloLayerWriter(
  val attributeStore: AttributeStore,
  instance: AccumuloInstance,
  table: String,
  options: AccumuloLayerWriter.Options
) extends LayerWriter[LayerId] with LazyLogging {

  // Layer Updating
  protected def _overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](
    sc: SparkContext,
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    keyBounds: KeyBounds[K]
  ): Unit = {
    _update(sc, id, rdd, keyBounds, None)
  }

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](
    sc: SparkContext,
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    keyBounds: KeyBounds[K],
    mergeFunc: (V, V) => V
  ): Unit = {
    _update(sc, id, rdd, keyBounds, Some(mergeFunc))
  }

  private def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](
    sc: SparkContext,
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    keyBounds: KeyBounds[K],
    mergeFunc: Option[(V, V) => V]
  ) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val table = header.tileTable

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    val encodeKey = (key: K) => AccumuloKeyEncoder.encode(id, key, keyIndex.toIndex(key))
    implicit val sc2: SparkContext = sc
    implicit val instance2 = instance
    val layerReader = new AccumuloLayerReader(attributeStore)

    logger.info(s"Saving updated RDD for layer ${id} to table $table")
    val existingTiles =
      if(schemaHasChanged[K, V](writerSchema)) {
        logger.warn(s"RDD schema has changed, this requires rewriting the entire layer.")
        layerReader
          .read[K, V, M](id)

      } else {
        val query =
          new LayerQuery[K, M]
            .where(Intersects(rdd.metadata.getComponent[Bounds[K]].get))

        layerReader.read[K, V, M](id, query, layerReader.defaultNumPartitions, filterIndexOnly = true)
      }

    val updatedMetadata: M =
      metadata.merge(rdd.metadata)

    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    options.writeStrategy match {
      case _: HdfsWriteStrategy =>
        val updatedRdd: RDD[(K, V)] =
          mergeFunc match {
            case Some(mergeFunc) =>
              existingTiles
                .fullOuterJoin(rdd)
                .flatMapValues {
                case (Some(layerTile), Some(updateTile)) => Some(mergeFunc(layerTile, updateTile))
                case (Some(layerTile), _) => Some(layerTile)
                case (_, Some(updateTile)) => Some(updateTile)
                case _ => None
              }
            case None => rdd
          }

        // Write updated metadata, and the possibly updated schema
        // Only really need to write the metadata and schema
        attributeStore.writeLayerAttributes(id, header, updatedMetadata, keyIndex, schema)
        AccumuloRDDWriter.write(updatedRdd, instance, encodeKey, options.writeStrategy, table)
      case _ =>
        // Write updated metadata, and the possibly updated schema
        // Only really need to write the metadata and schema
        attributeStore.writeLayerAttributes(id, header, updatedMetadata, keyIndex, schema)
        AccumuloRDDWriter.update(
          rdd, instance, encodeKey, options.writeStrategy, table,
          Some(writerSchema), mergeFunc
        )
    }
  }

  // Layer Writing
  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
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
