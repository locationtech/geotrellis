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

package geotrellis.store.json


import cats.syntax.either._
import geotrellis.store.json.Implicits._
import io.circe._
import io.circe.parser._
import org.apache.avro.Schema

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class SchemaJsonSpec extends AnyFunSpec with Matchers {
  describe("KeyIndexJsonFormatFactory") {
    val schemaString = "{\"type\":\"record\",\"name\":\"KeyValueRecord\",\"namespace\":\"geotrellis.spark.io\",\"fields\":[{\"name\":\"pairs\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"Tuple2\",\"namespace\":\"scala\",\"fields\":[{\"name\":\"_1\",\"type\":{\"type\":\"record\",\"name\":\"SpaceTimeKey\",\"namespace\":\"geotrellis.spark\",\"fields\":[{\"name\":\"col\",\"type\":\"int\"},{\"name\":\"row\",\"type\":\"int\"},{\"name\":\"instant\",\"type\":\"long\",\"aliases\":[\"millis\"]}]}},{\"name\":\"_2\",\"type\":{\"type\":\"record\",\"name\":\"ArrayMultibandTile\",\"namespace\":\"geotrellis.raster\",\"fields\":[{\"name\":\"bands\",\"type\":{\"type\":\"array\",\"items\":[{\"type\":\"record\",\"name\":\"ByteArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":\"bytes\"},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":-128}]},{\"type\":\"record\",\"name\":\"FloatArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":{\"type\":\"array\",\"items\":\"float\"}},{\"name\":\"noDataValue\",\"type\":[\"boolean\",\"float\"],\"default\":true}]},{\"type\":\"record\",\"name\":\"DoubleArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":{\"type\":\"array\",\"items\":\"double\"}},{\"name\":\"noDataValue\",\"type\":[\"boolean\",\"double\"],\"default\":true}]},{\"type\":\"record\",\"name\":\"ShortArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":-32768}]},{\"type\":\"record\",\"name\":\"IntArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":-2147483648}]},{\"type\":\"record\",\"name\":\"BitArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":\"bytes\"}]},{\"type\":\"record\",\"name\":\"UByteArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":\"bytes\"},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":0}]},{\"type\":\"record\",\"name\":\"UShortArrayTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cells\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":0}]},{\"type\":\"record\",\"name\":\"ByteConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"int\"},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":-128}]},{\"type\":\"record\",\"name\":\"FloatConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"float\"},{\"name\":\"noDataValue\",\"type\":[\"boolean\",\"float\"],\"default\":true}]},{\"type\":\"record\",\"name\":\"DoubleConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"double\"},{\"name\":\"noDataValue\",\"type\":[\"boolean\",\"double\"],\"default\":true}]},{\"type\":\"record\",\"name\":\"ShortConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"int\"},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":0}]},{\"type\":\"record\",\"name\":\"IntConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"int\"},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":-2147483648}]},{\"type\":\"record\",\"name\":\"BitConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"boolean\"}]},{\"type\":\"record\",\"name\":\"UByteConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"int\"},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":0}]},{\"type\":\"record\",\"name\":\"UShortConstantTile\",\"fields\":[{\"name\":\"cols\",\"type\":\"int\"},{\"name\":\"rows\",\"type\":\"int\"},{\"name\":\"cell\",\"type\":\"int\"},{\"name\":\"noDataValue\",\"type\":[\"int\",\"null\"],\"default\":0}]}]}}]}}]}}}]}"
    it("should be able to decode Schema from generic Json, NOT String") {
      val json: Json = parse(schemaString).valueOr(throw _)
      val schema = json.as[Schema].valueOr(throw _)
    }
  }
}

