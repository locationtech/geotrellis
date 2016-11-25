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

import geotrellis.spark.io.avro._
import org.apache.avro._
import org.apache.avro.generic.GenericRecord

class TupleCodec[A, B](implicit a: AvroRecordCodec[A], b: AvroRecordCodec[B]) extends AvroRecordCodec[(A, B)] {
  def schema = SchemaBuilder.record("Tuple2").namespace("scala")
    .fields()
    .name("_1").`type`(a.schema).noDefault()
    .name("_2").`type`(b.schema).noDefault()
  .endRecord()

  def encode(tuple: (A, B), rec: GenericRecord) = {
    rec.put("_1", a.encode(tuple._1))
    rec.put("_2", b.encode(tuple._2))
  }

  def decode(rec: GenericRecord) =
    a.decode(rec[GenericRecord]("_1")) -> b.decode(rec[GenericRecord]("_2"))
}

object TupleCodec {
  def apply[A: AvroRecordCodec, B: AvroRecordCodec] = new TupleCodec[A, B]
}
