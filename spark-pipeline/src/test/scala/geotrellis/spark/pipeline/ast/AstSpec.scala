package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json
import geotrellis.spark.pipeline.json._
import geotrellis.spark.pipeline.ast.untyped._
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutScheme}
import geotrellis.spark.testkit._

import org.scalatest._

import scala.util.{Failure, Try}

class AstSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with TestEnvironment {

  describe("Build AST") {
    it("should validate AST") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
      val read = HadoopRead(json.read.JsonRead("/", `type` = ReadTypes.SpatialHadoopType))
      val tiled = TileToLayout(read, json.transform.TileToLayout(`type` = TransformTypes.SpatialTileToLayoutType))
      val reproject = BufferedReproject(tiled, json.transform.Reproject("", scheme, `type` = TransformTypes.SpatialBufferedReprojectType))
      val pyramid = Pyramid(reproject, json.transform.Pyramid(`type` = TransformTypes.SpatialPyramidType))

      val write1 = HadoopWrite(pyramid, json.write.JsonWrite("write1", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType))
      val write2 = HadoopWrite(write1, json.write.JsonWrite("write2", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialHadoopType))

      println("------------------")
      println
      println(write1)
      println
      println(write2)
      println
      println("------------------")
      println
      println(write1.prettyPrint)
      println
      println(write2.prettyPrint)
      println
      println("------------------")
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

      val list = jsonRead ~ jsonTileToLayout ~ jsonReproject ~ jsonPyramid ~ jsonWrite1 ~ jsonWrite2

      val typedAst =
        list
          .node[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

      val untypedAst = list.erasedNode

      ErasedUtils.eprint(untypedAst)

      val typedAst2 =
        untypedAst.node[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

      println("------------------")
      println(typedAst.prettyPrint)
      println("------------------")

      typedAst shouldBe typedAst2
    }
  }

  it("Untyped JAST") {
    import geotrellis.spark.pipeline.ast.untyped.Implicits._
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

    val list: List[PipelineExpr] = js.pipelineExpr match {
      case Right(r) => r
      case Left(e) => throw e
    }

    val typedAst =
      list
        .node[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

    val untypedAst = list.erasedNode

    ErasedUtils.eprint(untypedAst)

    val typedAst2 =
      untypedAst.node[Write[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]]

    println("------------------")
    println(typedAst.prettyPrint)
    println("------------------")

    typedAst shouldBe typedAst2
  }

  it("Untyped JAST runnable") {
    import geotrellis.spark.pipeline.ast.untyped.Implicits._
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

    val list: List[PipelineExpr] = js.pipelineExpr match {
      case Right(r) => r
      case Left(e) => throw e
    }

    val erasedNode = list.erasedNode

    intercept[Exception] {
      Try { erasedNode.unsafeRun } match {
        case Failure(e) => println("unsafeRun failed as expected"); throw e
        case _ =>
      }
    }

    intercept[Exception] {
      Try {
        erasedNode.run[Stream[(Int, geotrellis.spark.TileLayerRDD[geotrellis.spark.SpatialKey])]]
      } match {
        case Failure(e) => println("run failed as expected"); throw e
        case _ =>
      }
    }
  }
}
