package geotrellis.spark.etl.config

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import geotrellis.raster.TileLayout
import geotrellis.spark.etl.config.json._
import org.scalatest.FunSuite
import spray.json._

/**
  * Created by meldridge on 5/31/17.
  */
class ConfigSpec extends FunSuite {

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
    val schemaFactory = JsonSchemaFactory.byDefault()
    val outputSchema = schemaFactory.getJsonSchema(JsonLoader.fromResource("/output-schema.json"))
    val report = outputSchema.validate(JsonLoader.fromString(outJson), true)
    println(report)
    assert(report.isSuccess)
  }
  test("Output config with TileLayout parses correctly") {
    val backendProfilesParsed = bpJson.parseJson.convertTo[Map[String, BackendProfile]]
    val outputParsed = OutputFormat(backendProfilesParsed).read(outJson.parseJson)
    assert(outputParsed.tileLayout.contains(TileLayout(360, 180, 240, 240)))
  }
}
