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

package geotrellis.layers.accumulo

import geotrellis.tiling.{Boundable, KeyBounds}
import geotrellis.layers._
import geotrellis.layers.accumulo.conf.AccumuloConfig
import geotrellis.layers.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.avro.{AvroEncoder, AvroRecordCodec}

import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.accumulo.core.security.Authorizations
import org.apache.avro.Schema
import org.apache.hadoop.io.Text

import cats.effect._
import cats.syntax.apply._

import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

import java.util.concurrent.Executors


object AccumuloCollectionReader {
  val defaultThreadCount: Int = AccumuloConfig.threads.collection.readThreads

  def read[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    table: String,
    columnFamily: Text,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[AccumuloRange],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = defaultThreadCount
  )(implicit instance: AccumuloInstance): Seq[(K, V)] = {
    if(queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val codec = KeyValueRecordCodec[K, V]
    val includeKey = (key: K) => queryKeyBounds.includeKey(key)

    val ranges = queryKeyBounds.flatMap(decomposeBounds).toIterator

    val pool = Executors.newFixedThreadPool(threads)
    implicit val ec = ExecutionContext.fromExecutor(pool)
    implicit val cs = IO.contextShift(ec)

    val range: fs2.Stream[IO, AccumuloRange] = fs2.Stream.fromIterator[IO, AccumuloRange](ranges)

    val read = { (range: AccumuloRange) => fs2.Stream eval IO.shift(ec) *> IO {
      val scanner = instance.connector.createScanner(table, new Authorizations())
      scanner.setRange(range)
      scanner.fetchColumnFamily(columnFamily)
      val result =
        scanner
          .iterator.asScala
          .map({ entry => AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), entry.getValue.get)(codec) })
          .flatMap({ pairs: Vector[(K, V)] =>
            if (filterIndexOnly) pairs
            else pairs.filter { pair => includeKey(pair._1) }
          }).toVector
        scanner.close()
        result
      }
    }

    try {
      range
        .map(read)
        .parJoin(threads)
        .compile
        .toVector
        .map(_.flatten)
        .unsafeRunSync
    } finally pool.shutdown()
  }
}
