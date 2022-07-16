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

package geotrellis.store.cassandra

import geotrellis.layer._
import geotrellis.store.util.IOUtils
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.store.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.store.index.MergeQueue
import geotrellis.store.LayerId
import geotrellis.store.util.IORuntimeTransient

import cats.effect._
import org.apache.avro.Schema
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal

import scala.reflect.ClassTag
import java.math.BigInteger

object CassandraCollectionReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: CassandraInstance,
    keyspace: String,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
  ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    implicit val ioRuntime: unsafe.IORuntime = runtime

    val query = QueryBuilder
      .selectFrom(keyspace, table)
      .column("value")
      .whereColumn("key").isEqualTo(QueryBuilder.bindMarker())
      .whereColumn("name").isEqualTo(literal(layerId.name))
      .whereColumn("zoom").isEqualTo(literal(layerId.zoom))
      .build()

    instance.withSessionDo { session =>
      val statement = session.prepare(query)

      IOUtils.parJoinIO[K, V](ranges.iterator) { index: BigInt =>
        session.executeF[IO](statement.bind(index.asJava)).map { row =>
          if (row.nonEmpty) {
            val bytes = row.one().getByteBuffer("value").array()
            val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
            if (filterIndexOnly) recs
            else recs.filter { row => includeKey(row._1) }
          } else Vector.empty
        }
      }
    }: Seq[(K, V)]
  }
}
