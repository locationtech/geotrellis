/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.dsl.avro

import geotrellis.store.avro._
import geotrellis.vector.{Point, Polygon}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.{Schema, SchemaBuilder}
import org.locationtech.jts.geom.Coordinate

import scala.collection.JavaConverters._

trait GeometryCodecs {
  implicit def coordinateCodec: AvroRecordCodec[Coordinate] = new AvroRecordCodec[Coordinate]() {
    override def schema: Schema = SchemaBuilder
      .record("Coordinate")
      .namespace("org.locationtech.jts.geom")
      .fields()
      .name("x").`type`.doubleType().noDefault
      .name("y").`type`.doubleType().noDefault
      .name("z").`type`.doubleType().noDefault
      .endRecord()

    override def encode(c: Coordinate, rec: GenericRecord): Unit = {
      rec.put("x", c.getX)
      rec.put("y", c.getY)
      rec.put("z", c.getZ)
    }

    override def decode(rec: GenericRecord): Coordinate = {
      new Coordinate(
        rec[Double]("x"),
        rec[Double]("y"),
        rec[Double]("z")
      )
    }
  }

  implicit def pointCodec: AvroRecordCodec[Point] = new AvroRecordCodec[Point] {
    def schema = SchemaBuilder
      .record("Point")
      .namespace("geotrellis.vector")
      .fields()
      .name("coordinate").`type`(AvroRecordCodec[Coordinate].schema).noDefault
      .name("srid").`type`.intType.noDefault
      .endRecord()

    def encode(thing: Point, rec: GenericRecord): Unit = {
      rec.put("coordinate", AvroRecordCodec[Coordinate].encode(thing.getCoordinate))
      rec.put("srid", thing.getSRID)
    }

    def decode(rec: GenericRecord): Point = {
      val coordinate = AvroRecordCodec[Coordinate].decode(rec[GenericRecord]("coordinate"))
      val point = Point(coordinate)
      point.setSRID(rec[Int]("srid"))
      point
    }
  }

  implicit def polygonCodec: AvroRecordCodec[Polygon] = new AvroRecordCodec[Polygon] {
    override def schema: Schema = SchemaBuilder
      .record("Polygon")
      .namespace("geotrellis.vector")
      .fields
      .name("coordinates").`type`.array.items(AvroRecordCodec[Coordinate].schema).noDefault
      .name("srid").`type`.intType.noDefault
      .endRecord

    override def encode(thing: Polygon, rec: GenericRecord): Unit = {
      val coordinates = java.util.Arrays.asList(thing.getCoordinates.map(coordinateCodec.encode): _*)
      rec.put("coordinates", coordinates)
      rec.put("srid", thing.getSRID)
    }

    override def decode(rec: GenericRecord): Polygon = {
      val coordinates = rec.get("coordinates")
        .asInstanceOf[java.util.Collection[GenericRecord]]
        .asScala
        .toArray[GenericRecord]
        .map(coordinateCodec.decode)
      val polygon = Polygon(coordinates)
      polygon.setSRID(rec[Int]("srid"))
      polygon
    }
  }
}
