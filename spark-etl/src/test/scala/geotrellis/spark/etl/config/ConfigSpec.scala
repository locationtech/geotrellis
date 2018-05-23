package geotrellis.spark.etl.config

import io.circe._
import io.circe.parser._
import cats.syntax.either._

import geotrellis.raster.TileLayout
import geotrellis.spark.etl.config.json._

import com.networknt.schema.JsonSchemaFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import org.scalatest.FunSuite

/**
  * Created by meldridge on 5/31/17.
  */
class ConfigSpec extends FunSuite {

  def jsonNodeFromString(content: String): JsonNode = new ObjectMapper().readTree(content)

  val bpJson =
    """
      |{
      |  "backend-profiles":[
      |    {
      |      "name":"accumulo-profile",
      |      "type":"accumulo",
      |      "zookeepers":"zooky",
      |      "instance":"abcde",
      |      "user":"donald",
      |      "password":"covfefe"
      |    }
      |  ]
      |}
    """.stripMargin

  val outJson =
    """
      |{
      |  "backend":{
      |        "type":"accumulo",
      |        "path":"TEST-TABLE",
      |        "profile":"accumulo-profile"
      |      },
      |      "reprojectMethod":"per-tile",
      |      "encoding":"geotiff",
      |      "pyramid":false,
      |      "resampleMethod":"nearest-neighbor",
      |      "keyIndexMethod":{
      |        "type":"zorder",
      |        "temporalResolution": 86400000
      |      },
      |      "tileLayout": {
      |        "layoutCols": 360,
      |        "layoutRows": 180,
      |        "tileCols":   240,
      |        "tileRows":   240
      |      },
      |      "tileSize":256,
      |      "crs":"ESRI:54008"
      |}
    """.stripMargin

  test("Output config with TileLayout passes schema checks") {
    val schemaFactory = JsonSchemaFactory.getInstance()
    val outputSchema = schemaFactory.getSchema(getClass.getResourceAsStream("/output-schema.json"))
    val report = outputSchema.validate(jsonNodeFromString(outJson))
    println(report)
    assert(report.isEmpty)
  }
  test("Output config with TileLayout parses correctly") {
    val backendProfilesParsed = parse(bpJson).flatMap(_.as[Map[String, BackendProfile]]).valueOr(throw _)
    val outputParsed = Output.OutputDecoder(backendProfilesParsed)(parse(outJson).map(_.hcursor).valueOr(throw _)).valueOr(throw _)
    assert(outputParsed.tileLayout.contains(TileLayout(360, 180, 240, 240)))
  }
}
