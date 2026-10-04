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

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.{InflaterInputStream, DeflaterOutputStream}
import org.apache.avro.generic.*
import org.apache.avro.io.*
import org.apache.avro.*
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.ByteArrayOutputStream

import scala.util.Using

object AvroEncoder {
  /**
    * Avro compiles readers per schema instance (identity), and the Avro 1.12 fast reader never releases them.
    * Keep one canonical instance per distinct schema and one reader / writer per canonical schema,
    * so that work happens once per schema (the same approach as Spark's GenericAvroSerializer).
    */
  private val schemas = new ConcurrentHashMap[Schema, Schema]()
  private val readers = new ConcurrentHashMap[(Schema, Schema), GenericDatumReader[GenericRecord]]()
  private val writers = new ConcurrentHashMap[Schema, GenericDatumWriter[GenericRecord]]()

  private def canonical(schema: Schema): Schema = {
    val prev = schemas.putIfAbsent(schema, schema)
    if (prev == null) schema else prev
  }

  private def datumReader(writerSchema: Schema, readerSchema: Schema): GenericDatumReader[GenericRecord] =
    readers.computeIfAbsent(
      (canonical(writerSchema), canonical(readerSchema)),
      { case (key, value) => new GenericDatumReader[GenericRecord](key, value) }
    )

  private def datumWriter(schema: Schema): GenericDatumWriter[GenericRecord] =
    writers.computeIfAbsent(canonical(schema), new GenericDatumWriter[GenericRecord](_))

  def compress(bytes: Array[Byte]): Array[Byte] = {
    val baos = new ByteArrayOutputStream(bytes.length)
    // close the stream-owned Deflater to free its native zlib memory
    Using.resource(new DeflaterOutputStream(baos))(_.write(bytes))
    baos.toByteArray
  }

  def decompress(bytes: Array[Byte]): Array[Byte] =
    // close the stream-owned Inflater to free its native zlib memory
    Using.resource(new InflaterInputStream(new ByteArrayInputStream(bytes)))(IOUtils.toByteArray)

  def toBinary[T: AvroRecordCodec](thing: T): Array[Byte] =
    toBinary(thing, deflate = true)

  def toBinary[T: AvroRecordCodec](thing: T, deflate: Boolean): Array[Byte] = {
    val format = AvroRecordCodec[T]
    val schema: Schema = format.schema

    val writer = datumWriter(schema)
    val jos = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(jos, null)
    writer.write(format.encode(thing), encoder)
    encoder.flush()
    if (deflate)
      compress(jos.toByteArray)
    else
      jos.toByteArray
  }

  def fromBinary[T: AvroRecordCodec](bytes: Array[Byte]): T =
    fromBinary[T](AvroRecordCodec[T].schema, bytes)

  def fromBinary[T: AvroRecordCodec](bytes: Array[Byte], uncompress: Boolean): T =
    fromBinary[T](AvroRecordCodec[T].schema, bytes, uncompress)

  def fromBinary[T: AvroRecordCodec](writerSchema: Schema, bytes: Array[Byte]): T =
    fromBinary(writerSchema, bytes, uncompress = true)

  def fromBinary[T: AvroRecordCodec](writerSchema: Schema, bytes: Array[Byte], uncompress: Boolean): T = {
    val format = AvroRecordCodec[T]
    val schema = format.schema

    val reader = datumReader(writerSchema, schema)
    val decoder =
      if (uncompress)
        DecoderFactory.get().binaryDecoder(decompress(bytes), null)
      else
        DecoderFactory.get().binaryDecoder(bytes, null)
    try {
      val rec = reader.read(null.asInstanceOf[GenericRecord], decoder)
      format.decode(rec)
    } catch {
      case e: AvroTypeException =>
        throw new AvroTypeException(e.getMessage + ". " +
          "This can be caused by using a type parameter which doesn't match the object being deserialized.", e)
    }
  }

  def toJson[T: AvroRecordCodec](thing: T): String = {
    val format = AvroRecordCodec[T]
    val schema = format.schema

    val writer = datumWriter(schema)
    val jos = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().jsonEncoder(schema, jos)
    writer.write(format.encode(thing), encoder)
    encoder.flush()
    jos.toString(StandardCharsets.UTF_8)
  }

  def fromJson[T: AvroRecordCodec](json: String): T = {
    val format = AvroRecordCodec[T]
    val schema = format.schema

    val reader = datumReader(schema, schema)
    val decoder = DecoderFactory.get().jsonDecoder(schema, json)
    try {
      val rec = reader.read(null.asInstanceOf[GenericRecord], decoder)
      format.decode(rec)
    } catch {
      case e: AvroTypeException =>
        throw new AvroTypeException(e.getMessage + ". " +
          "This can be caused by using a type parameter which doesn't match the object being deserialized.", e)
    }
  }
}
