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

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.tiling.SpatialComponent
import geotrellis.spark.io._
import geotrellis.layers.io.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.io.avro.{AvroEncoder, AvroRecordCodec}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.{eq => eqs}
import spray.json._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import java.math.BigInteger

import geotrellis.layers.LayerId
import geotrellis.layers.io.{OverzoomingValueReader, Reader}

class CassandraValueReader(
  instance: CassandraInstance,
  val attributeStore: AttributeStore
) extends OverzoomingValueReader {

  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[CassandraLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)
    val codec = KeyValueRecordCodec[K, V]

    private lazy val statement = instance.withSession{ session =>
      session.prepare(
        QueryBuilder.select("value")
          .from(header.keyspace, header.tileTable)
          .where(eqs("key", QueryBuilder.bindMarker()))
          .and(eqs("name", layerId.name))
          .and(eqs("zoom", layerId.zoom))
      )
    }

    def read(key: K): V = instance.withSession { session =>
      val row = session.execute(statement.bind(keyIndex.toIndex(key): BigInteger)).all()
      val tiles = row.asScala.map { entry =>
          AvroEncoder.fromBinary(writerSchema, entry.getBytes("value").array())(codec)
        }
        .flatMap { pairs: Vector[(K, V)] =>
          pairs.filter(pair => pair._1 == key)
        }
        .toVector

      if (tiles.isEmpty) {
        throw new ValueNotFoundError(key, layerId)
      } else if (tiles.size > 1) {
        throw new LayerIOError(s"Multiple values (${tiles.size}) found for $key for layer $layerId")
      } else {
        tiles.head._2
      }
    }
  }
}

object CassandraValueReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    instance: CassandraInstance,
    attributeStore: AttributeStore,
    layerId: LayerId
  ): Reader[K, V] =
    new CassandraValueReader(instance, attributeStore).reader[K, V](layerId)

  def apply[K: AvroRecordCodec: JsonFormat: SpatialComponent: ClassTag, V <: CellGrid[Int]: AvroRecordCodec: ? => TileResampleMethods[V]](
    instance: CassandraInstance,
    attributeStore: AttributeStore,
    layerId: LayerId,
    resampleMethod: ResampleMethod
  ): Reader[K, V] =
    new CassandraValueReader(instance, attributeStore).overzoomingReader[K, V](layerId, resampleMethod)

  def apply(instance: CassandraInstance): CassandraValueReader =
    new CassandraValueReader(
      instance = instance,
      attributeStore = CassandraAttributeStore(instance))

  def apply(attributeStore: CassandraAttributeStore): CassandraValueReader =
    new CassandraValueReader(
      instance = attributeStore.instance,
      attributeStore = attributeStore)
}
