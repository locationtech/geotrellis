/*
 * Copyright 2021 Azavea
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

package geotrellis.geowave.dsl

import geotrellis.geowave.dsl.syntax._
import geotrellis.geowave.adapter.geotiff.GeoTiffAdapter.GEOTIFF_TIME_FORMATTER_DEFAULT
import geotrellis.geowave.dsl.json.{JsonValidator, JsonValidatorErrors}

import cats.syntax.option._
import io.circe.syntax._
import org.locationtech.geowave.core.index.sfc.data.NumericRange
import org.locationtech.geowave.core.geotime.store.dimension.Time.TimeRange
import org.locationtech.geowave.core.store.query.filter.BasicQueryFilter
import geotrellis.vector._

import java.net.URI
import java.time.ZonedDateTime

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import _root_.io.circe.JsonObject
import _root_.io.circe.Codec

class MessagesSpec extends AnyFunSpec with Matchers {
  describe("MessagesSpec") {

    it("should build GeoTiff ingest and index messages") {
      val ingestParameters = IngestParameters[TilingBounds](
        typeName     = "GeoTiffTest".typeName,
        dataType     = "GeoTiff".dataType,
        uri          = new URI("file://path/to/file"),
        options      = TilingBounds(depth = 1.some).some,
        namespace    = "GeoTiffTestMessagesSpec".some
      )

      val indexParameters = IndexParameters(
        indices = IndexDefinition(
          indexName    = None,
          indexType    = "spatial-temporal-depth".indexType,
          indexOptions = Map("periodTemporal" -> "Year", "maxDepth" -> "100")
        ),
        // should be used in the ingest message as a name to create an instance of adapter with a proper index
        typeName     = "GeoTiffTest".typeName,
        dataType     = "GeoTiff".dataType,
        namespace = "GeoTiffTestMessagesSpec".some
      )
      ingestParameters.asJson shouldBe JsonValidator.parseUnsafe[IngestParameters[TilingBounds]](ingestParameters.asJson.spaces4).asJson
      indexParameters.asJson shouldBe JsonValidator.parseUnsafe[IndexParameters](indexParameters.asJson.spaces4).asJson
    }

    it("should not build uingest and index messages for the unsupported type") {
      val exception = intercept[JsonValidatorErrors] {
        val ingestParameters = IngestParameters[TilingBounds](
          typeName = "NewTest".typeName,
          dataType = "New".dataType,
          uri = new URI("file://path/to/file"),
          options = TilingBounds(
            depth = 1.some,
            spissitude = 1.some
          ).some,
          namespace = "NewTestMessagesSpec".some
        )

        val indexParameters = IndexParameters(
          indices = IndexDefinition(
            indexName = None,
            indexType = "spatial-temporal-depth".indexType,
            indexOptions = Map("periodTemporal" -> "Year", "maxDepth" -> "100")
          ),
          // should be used in the ingest message as a name to create an instance of adapter with a proper index
          typeName = "NewTest".typeName,
          dataType = "New".dataType,
          namespace = "NewTestMessagesSpec".some
        )

        ingestParameters.asJson shouldBe JsonValidator.parseUnsafe[IngestParameters[String]](ingestParameters.asJson.spaces4).asJson
        indexParameters.asJson shouldBe JsonValidator.parseUnsafe[IndexParameters](indexParameters.asJson.spaces4).asJson
      }

      exception.getMessage shouldBe "Invalid DataType: New; available DataTypes: GeoTiff: DownField(dataType)"
    }
  }

  it("should parse delete message") {
    val deleteExpected = """
                           |{
                           |    "typeName" : "DeleteTest",
                           |    "indexName": "TestIndex",
                           |    "geometry" : {
                           |        "type" : "Point",
                           |        "coordinates" : [
                           |            1.0,
                           |            1.0
                           |        ]
                           |    },
                           |    "namespace" : "testnamesapce",
                           |    "time" : {
                           |        "min" : "1970-01-01T00:00:00Z",
                           |        "max" : "2019-11-01T12:00:00Z"
                           |    },
                           |    "elevation" : {
                           |        "min" : 0.0,
                           |        "max" : 30000.0
                           |    },
                           |    "compareOp" : "INTERSECTS"
                           |}
                           |""".stripMargin


    val date = ZonedDateTime.from(GEOTIFF_TIME_FORMATTER_DEFAULT.parse("2019:11:01 12:00:00")).toInstant

    val deleteParameters = DeleteParameters(
      typeName  = "DeleteTest".typeName,
      indexName = "TestIndex",
      geometry  = Point(1, 1).some,
      namespace = "testnamesapce".some,
      time      = new TimeRange(0, date.toEpochMilli).some,
      elevation     = new NumericRange(0d, 30000d).some,
      compareOp = BasicQueryFilter.BasicQueryCompareOperation.INTERSECTS
    )

    deleteParameters.asJson shouldBe JsonValidator.parseUnsafe[DeleteParameters](deleteExpected).asJson
  }

  it("should not parse incorrect IndexParameters") {
    JsonValidator.parse[IndexParameters]("{}") match {
      case Right(_) => throw new Exception("This test should fail.")
      case Left(e) => e.toList.map(_.getMessage) shouldBe List(
        "#: 3 schema violations found",
        "#: required key [indices] not found",
        "#: required key [typeName] not found",
        "#: required key [dataType] not found"
      )
    }
  }

  it("should not parse incorrect IngestParameters") {
    JsonValidator.parse[IngestParameters[String]]("{}") match {
      case Right(_) => throw new Exception("This test should fail.")
      case Left(e) => e.toList.map(_.getMessage) shouldBe List(
        "#: 3 schema violations found",
        "#: required key [typeName] not found",
        "#: required key [dataType] not found",
        "#: required key [uri] not found"
      )
    }
  }

  it("should not parse incorrect DeleteParameters") {
    JsonValidator.parse[DeleteParameters]("{}") match {
      case Right(_) => throw new Exception("This test should fail.")
      case Left(e) => e.toList.map(_.getMessage) shouldBe List(
        "#: 2 schema violations found",
        "#: required key [typeName] not found",
        "#: required key [indexName] not found"
      )
    }
  }
}
