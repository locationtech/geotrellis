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

package geotrellis.spark.io.avro

import org.apache.avro._
import org.apache.avro.generic._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * Restricted to be a union of Records that share a super type.
 * @param formats list of formats that make up the union
 * @tparam T      superclass of listed formats
 */
class AvroUnionCodec[T: ClassTag](formats: AvroRecordCodec[X] forSome {type X <: T} *) extends AvroRecordCodec[T] {
  def schema: Schema =
    Schema.createUnion(formats.map(_.schema).asJava)

  override
  def encode(thing: T): GenericRecord = {
    val format = findFormat(_.supported(thing), thing.getClass.toString)
    val rec = new GenericData.Record(format.schema)
    format.encode(thing, rec)
    rec
  }

  def encode(thing: T, rec: GenericRecord) = {
    findFormat(_.supported(thing), thing.getClass.toString).encode(thing, rec)
  }

  def decode(rec: GenericRecord): T = {
    val fullName = rec.getSchema.getFullName
    findFormat(_.schema.getFullName == fullName, fullName).decode(rec)
  }

  private def findFormat(f: AvroRecordCodec[_] => Boolean, target: String): AvroRecordCodec[T] =
    formats.filter(f) match {
      case Seq(format) => format.asInstanceOf[AvroRecordCodec[T]]
      case Seq() => sys.error(s"No formats found to support $target")
      case list => sys.error(s"Multiple formats support $target: ${list.toList}")
    }

}
