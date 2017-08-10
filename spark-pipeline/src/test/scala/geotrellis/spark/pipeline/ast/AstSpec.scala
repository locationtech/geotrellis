package geotrellis.spark.pipeline.ast

import _root_.io.circe.generic.extras.ConfiguredJsonCodec
import _root_.io.circe.syntax._
import _root_.io.circe.parser._
import cats.implicits._
import geotrellis.spark._
import geotrellis.spark.pipeline.ast.untyped.ErasedJsonNode
import geotrellis.spark.pipeline.json
import geotrellis.spark.pipeline.json._
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutScheme}
import geotrellis.spark.testkit._
import geotrellis.spark.pipeline.ast.untyped._
import geotrellis.spark.pipeline.json.write.JsonWrite
import org.apache.spark.SparkContext
import org.scalatest._

class AstSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with TestEnvironment {

  describe("Build AST") {
    /*@ConfiguredJsonCodec
    case class NewReproject(`type`: String, args: List[String]) extends PipelineExpr

    case class NewReprojectTransform(
      node: Node[TileLayerRDD[SpatialKey]],
      arg: NewReproject
    ) extends Transform[TileLayerRDD[SpatialKey], TileLayerRDD[SpatialKey]] {
      def get(implicit sc: SparkContext): TileLayerRDD[SpatialKey] = {
        // some logic of cusom reprojection here
        null
      }

      // some validation function
      def validate: (Boolean, String) = {
        val (f, msg) = if (node == null) (false, s"${this.getClass} has no node")
        else node.validation
        val (fs, msgs) = validation
        (f && fs, msgs ++ msg)
      }

      def asJson = arg.asJson +: node.asJson
    }*/

    it("should validate AST") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
      val read = HadoopRead(json.read.JsonRead("/", `type` = ReadTypes.SpatialHadoopType))
      val tiled = TileToLayout(read, json.transform.TileToLayout(`type` = TransformTypes.SpatialTileToLayoutType))
      val reproject = BufferedReproject(tiled, json.transform.Reproject("", scheme, `type` = TransformTypes.SpatialBufferedReprojectType))
      //val reprojectn = NewReprojectTransform(tiled, NewReproject("id", List()))
      val pyramid = Pyramid(reproject, json.transform.Pyramid(`type` = TransformTypes.SpatialPyramidType))
      //val pyramidn = Pyramid(reprojectn, json.transform.Pyramid(`type` = TransformTypes.SpatialPyramidType))

      val write1 = HadoopWrite(pyramid, json.write.JsonWrite("write1", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType))
      val write2 = HadoopWrite(write1, json.write.JsonWrite("write2", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType))
      val write3 = HadoopWrite(write2, null)
      //val write1n = HadoopWrite(pyramidn, json.write.JsonWrite("write1n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType))
      //val write2n = HadoopWrite(write1n, json.write.JsonWrite("write2n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType))
      //val write3n = HadoopWrite(write2n, null)

      println
      println
      println(write1)
      println
      println(write2)
      println
      println(write3.validation)
      println
      println
      /*println(write1n)
      println
      println(write2n)
      println
      println(write3n.validation)
      println
      println*/


      println
      println
      println(write1.asJson.map(_.pretty(jsonPrinter)))
      println
      println(write2.asJson.map(_.pretty(jsonPrinter)))
      println
      println(write3.validation)
      println
      println
    }

    it("Untyped AST") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
      val jsonRead = json.read.JsonRead("/", `type` = ReadTypes.SpatialHadoopType)
      val jsonTileToLayout = json.transform.TileToLayout(`type` = TransformTypes.SpatialTileToLayoutType)
      val jsonReproject = json.transform.Reproject("", scheme, `type` = TransformTypes.SpatialBufferedReprojectType)
      val jsonPyramid = json.transform.Pyramid(`type` = TransformTypes.SpatialPyramidType)

      val jsonWrite1 = json.write.JsonWrite("write1", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType)
      val jsonWrite2 = json.write.JsonWrite("write2", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType)

      /*val njsonRead = ErasedJsonNode(jsonRead)
      val njsonTileToLayout = ErasedJsonNode(jsonTileToLayout)
      val njsonReproject = ErasedJsonNode(jsonReproject)
      val njsonPyramid = ErasedJsonNode(jsonPyramid)
      val njsonWrite1 = ErasedJsonNode(jsonWrite1)
      val njsonWrite2 = ErasedJsonNode(jsonWrite2)*/

      val list: List[PipelineExpr] = List(jsonRead, jsonTileToLayout, jsonReproject, jsonPyramid, jsonWrite1, jsonWrite2)

      val typedAst =
        list
          .get[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

      val untypedAst = list.typeErased

      ErasedUtils.eprint(untypedAst)

      val typedAst2 =
        untypedAst.get[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

      println(typedAst.validation)

      typedAst shouldBe typedAst2
    }
  }

  it("Untyped JAST") {
    val js: String =
      """
        |[
        |  {
        |    "uri" : "/",
        |    "time_tag" : "TIFFTAG_DATETIME",
        |    "time_format" : "yyyy:MM:dd HH:mm:ss",
        |    "type" : "singleband.spatial.read.hadoop"
        |  },
        |  {
        |    "resample_method" : "nearest-neighbor",
        |    "type" : "singleband.spatial.transform.tile-to-layout"
        |  },
        |  {
        |    "crs" : "",
        |    "scheme" : {
        |      "tileCols" : 512,
        |      "tileRows" : 512
        |    },
        |    "resample_method" : "nearest-neighbor",
        |    "type" : "singleband.spatial.transform.buffered-reproject"
        |  },
        |  {
        |    "end_zoom" : 0,
        |    "resample_method" : "nearest-neighbor",
        |    "type" : "singleband.spatial.transform.pyramid"
        |  },
        |  {
        |    "name" : "write1",
        |    "uri" : "/tmp",
        |    "pyramid" : true,
        |    "key_index_method" : {
        |      "type" : "zorder"
        |    },
        |    "scheme" : {
        |      "tileCols" : 512,
        |      "tileRows" : 512
        |    },
        |    "type" : "singleband.spatial.write.hadoop"
        |  }
        |]
      """.stripMargin

    val list = decode[List[PipelineExpr]](js) match {
      case Right(r) => r
      case Left(e) => throw e
    }

    val typedAst =
      list
        .get[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

    val untypedAst = list.typeErased

    ErasedUtils.eprint(untypedAst)

    val typedAst2 =
      untypedAst.get[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

    println(typedAst.validation)

    typedAst shouldBe typedAst2
  }
}
