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

package geotrellis.spark.io.avro.codecs

import geotrellis.spark.io.avro.AvroRecordCodec
import org.apache.avro._
import org.apache.avro.generic.GenericRecord

import scala.collection.JavaConverters._

class KeyValueRecordCodec[K, V](implicit a: AvroRecordCodec[K], b: AvroRecordCodec[V]) extends AvroRecordCodec[Vector[(K, V)]] {
  val pairCodec = new TupleCodec[K,V]

  def schema = SchemaBuilder
    .record("KeyValueRecord").namespace("geotrellis.spark.io")
    .fields()
    .name("pairs").`type`().array().items.`type`(pairCodec.schema).noDefault()
    .endRecord()

  def encode(t: Vector[(K, V)], rec: GenericRecord) = {
    rec.put("pairs", t.map(pairCodec.encode).asJavaCollection)
  }

  def decode(rec: GenericRecord) = {
    rec.get("pairs")
      .asInstanceOf[java.util.Collection[GenericRecord]]
      .asScala
      .map(pairCodec.decode)
      .toVector
  }
}

object KeyValueRecordCodec {
  def apply[K: AvroRecordCodec, V: AvroRecordCodec] = new KeyValueRecordCodec[K, V]
}
