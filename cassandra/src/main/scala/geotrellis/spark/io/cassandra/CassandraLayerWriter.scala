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

class CassandraLayerWriter(
  val attributeStore: AttributeStore,
  instance: CassandraInstance,
  keyspace: String,
  table: String
) extends LayerWriter[LayerId] with LazyLogging {

  // Layer updating
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

  def _update[
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
      attributeStore.readLayerAttributes[CassandraLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val (keyspace, table) = header.keyspace -> header.tileTable

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    val encodeKey = (key: K) => keyIndex.toIndex(key)
    implicit val sparkContext = sc
    val layerReader = new CassandraLayerReader(attributeStore, instance)

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

    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    // Write updated metadata, and the possibly updated schema
    // Only really need to write the metadata and schema
    attributeStore.writeLayerAttributes(id, header, updatedMetadata, keyIndex, schema)
    CassandraRDDWriter.write(updatedRdd, instance, id, encodeKey, keyspace, table)
  }

  // Layer writing
  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
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
