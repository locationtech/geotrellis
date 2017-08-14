package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline._
import geotrellis.spark.pipeline.json
import geotrellis.spark.pipeline.json._
import geotrellis.spark.pipeline.ast.untyped._
import geotrellis.spark._
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutScheme}
import geotrellis.spark.testkit._

import _root_.io.circe.syntax._
import _root_.io.circe.parser._
import cats.implicits._

import org.scalatest._

import scala.util.{Failure, Try}

class AstSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with TestEnvironment {

  val expectedWrite1 =
    """
      |[
      |  {
      |    "uri" : "/",
      |    "crs" : null,
      |    "tag" : null,
      |    "max_tile_size" : null,
      |    "partitions" : null,
      |    "partition_bytes" : null,
      |    "chunk_size" : null,
      |    "delimiter" : null,
      |    "time_tag" : "TIFFTAG_DATETIME",
      |    "time_format" : "yyyy:MM:dd HH:mm:ss",
      |    "type" : "singleband.spatial.read.hadoop"
      |  },
      |  {
      |    "resample_method" : "nearest-neighbor",
      |    "tile_size" : null,
      |    "cell_type" : null,
      |    "type" : "singleband.spatial.transform.tile-to-layout"
      |  },
      |  {
      |    "crs" : "",
      |    "scheme" : {
      |      "tileCols" : 512,
      |      "tileRows" : 512
      |    },
      |    "resample_method" : "nearest-neighbor",
      |    "max_zoom" : null,
      |    "type" : "singleband.spatial.transform.buffered-reproject"
      |  },
      |  {
      |    "start_zoom" : null,
      |    "end_zoom" : 0,
      |    "resample_method" : "nearest-neighbor",
      |    "type" : "singleband.spatial.transform.pyramid"
      |  },
      |  {
      |    "name" : "write1",
      |    "uri" : "/tmp",
      |    "key_index_method" : {
      |      "type" : "zorder",
      |      "time_tag" : null,
      |      "time_format" : null,
      |      "temporal_resolution" : null
      |    },
      |    "scheme" : {
      |      "tileCols" : 512,
      |      "tileRows" : 512
      |    },
      |    "profile" : null,
      |    "type" : "singleband.spatial.write"
      |  }
      |]
    """.stripMargin

  val expectedWrite1Json = parse(expectedWrite1).valueOr(throw _)

  val expectedWrite2 =
    """
      |[
      |  {
      |    "uri" : "/",
      |    "crs" : null,
      |    "tag" : null,
      |    "max_tile_size" : null,
      |    "partitions" : null,
      |    "partition_bytes" : null,
      |    "chunk_size" : null,
      |    "delimiter" : null,
      |    "time_tag" : "TIFFTAG_DATETIME",
      |    "time_format" : "yyyy:MM:dd HH:mm:ss",
      |    "type" : "singleband.spatial.read.hadoop"
      |  },
      |  {
      |    "resample_method" : "nearest-neighbor",
      |    "tile_size" : null,
      |    "cell_type" : null,
      |    "type" : "singleband.spatial.transform.tile-to-layout"
      |  },
      |  {
      |    "crs" : "",
      |    "scheme" : {
      |      "tileCols" : 512,
      |      "tileRows" : 512
      |    },
      |    "resample_method" : "nearest-neighbor",
      |    "max_zoom" : null,
      |    "type" : "singleband.spatial.transform.buffered-reproject"
      |  },
      |  {
      |    "start_zoom" : null,
      |    "end_zoom" : 0,
      |    "resample_method" : "nearest-neighbor",
      |    "type" : "singleband.spatial.transform.pyramid"
      |  },
      |  {
      |    "name" : "write1",
      |    "uri" : "/tmp",
      |    "key_index_method" : {
      |      "type" : "zorder",
      |      "time_tag" : null,
      |      "time_format" : null,
      |      "temporal_resolution" : null
      |    },
      |    "scheme" : {
      |      "tileCols" : 512,
      |      "tileRows" : 512
      |    },
      |    "profile" : null,
      |    "type" : "singleband.spatial.write"
      |  },
      |  {
      |    "name" : "write2",
      |    "uri" : "/tmp",
      |    "key_index_method" : {
      |      "type" : "zorder",
      |      "time_tag" : null,
      |      "time_format" : null,
      |      "temporal_resolution" : null
      |    },
      |    "scheme" : {
      |      "tileCols" : 512,
      |      "tileRows" : 512
      |    },
      |    "profile" : null,
      |    "type" : "singleband.spatial.write"
      |  }
      |]
    """.stripMargin

  val expectedWrite2Json = parse(expectedWrite2).valueOr(throw _)

  describe("Build AST") {
    it("should validate AST") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
      val read = HadoopRead(json.read.JsonRead("/", `type` = ReadTypes.SpatialHadoopType))
      val tiled = TileToLayout(read, json.transform.TileToLayout(`type` = TransformTypes.SpatialTileToLayoutType))
      val reproject = BufferedReproject(tiled, json.transform.Reproject("", scheme, `type` = TransformTypes.SpatialBufferedReprojectType))
      val pyramid = Pyramid(reproject, json.transform.Pyramid(`type` = TransformTypes.SpatialPyramidType))

      val write1 = Write(pyramid, json.write.JsonWrite("write1", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialType))
      val write2 = Write(write1, json.write.JsonWrite("write2", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialType))

      // println(write1.prettyPrint)
      // println(write2.prettyPrint)

      write1.asJson.asJson shouldBe expectedWrite1Json
      write2.asJson.asJson shouldBe expectedWrite2Json
    }

    it("Untyped AST") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
      val jsonRead = json.read.JsonRead("/", `type` = ReadTypes.SpatialHadoopType)
      val jsonTileToLayout = json.transform.TileToLayout(`type` = TransformTypes.SpatialTileToLayoutType)
      val jsonReproject = json.transform.Reproject("", scheme, `type` = TransformTypes.SpatialBufferedReprojectType)
      val jsonPyramid = json.transform.Pyramid(`type` = TransformTypes.SpatialPyramidType)

      val jsonWrite1 = json.write.JsonWrite("write1", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialType)
      val jsonWrite2 = json.write.JsonWrite("write2", "/tmp", PipelineKeyIndexMethod("zorder"), scheme, `type` = WriteTypes.SpatialType)

      val list = jsonRead ~ jsonTileToLayout ~ jsonReproject ~ jsonPyramid ~ jsonWrite1 ~ jsonWrite2

      val typedAst =
        list
          .node[Stream[(Int, TileLayerRDD[SpatialKey])]]

      val untypedAst = list.erasedNode

      ErasedUtils.eprint(untypedAst)

      val typedAst2 =
        untypedAst.node[Stream[(Int, TileLayerRDD[SpatialKey])]]

      // println(typedAst.prettyPrint)
      // println(typedAst2.prettyPrint)

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
        |    "type" : "singleband.spatial.write"
        |  }
        |]
      """.stripMargin

    val list: List[PipelineExpr] = js.pipelineExpr match {
      case Right(r) => r
      case Left(e) => throw e
    }

    val typedAst =
      list
        .node[Stream[(Int, TileLayerRDD[SpatialKey])]]

    val untypedAst = list.erasedNode

    ErasedUtils.eprint(untypedAst)

    val typedAst2 =
      untypedAst.node[Stream[(Int, TileLayerRDD[SpatialKey])]]

    // println(typedAst.prettyPrint)
    // println(typedAst2.prettyPrint)

    typedAst shouldBe typedAst2
  }

  it("Untyped JAST runnable") {
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
        |    "type" : "singleband.spatial.write"
        |  }
        |]
      """.stripMargin

    val list: List[PipelineExpr] = js.pipelineExpr match {
      case Right(r) => r
      case Left(e) => throw e
    }

    val erasedNode = list.erasedNode

    intercept[Exception] {
      Try { erasedNode.unsafeEval } match {
        case Failure(e) => println("unsafeRun failed as expected"); throw e
        case _ =>
      }
    }

    intercept[Exception] {
      Try {
        erasedNode.eval[Stream[(Int, TileLayerRDD[SpatialKey])]]
      } match {
        case Failure(e) => println("run failed as expected"); throw e
        case _ =>
      }
    }
  }
}
