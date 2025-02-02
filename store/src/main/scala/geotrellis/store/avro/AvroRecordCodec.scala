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

package geotrellis.store.avro

import org.apache.avro.generic._
import org.apache.avro._
import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

@implicitNotFound("Cannot find AvroRecordCodec for ${T}. Try to import geotrellis.store.avro.codecs.Implicits._")
abstract class AvroRecordCodec[T: ClassTag] extends AvroCodec[T, GenericRecord] {
  def schema: Schema
  def encode(thing: T, rec: GenericRecord): Unit
  def decode(rec: GenericRecord): T

  def encode(thing: T): GenericRecord = {
    val rec = new GenericData.Record(schema)
    encode(thing, rec)
    rec
  }

  def supported[O](other: O): Boolean = {
    implicitly[ClassTag[T]].unapply(other).isDefined
  }
}

object AvroRecordCodec {
  def apply[T: AvroRecordCodec]: AvroRecordCodec[T] = implicitly
}
