package geotrellis.spark.pipeline.eval

import geotrellis.raster.Tile
import geotrellis.spark.{SpatialKey, TileLayerRDD}
import geotrellis.spark.pipeline.eval.ast._
import geotrellis.spark.pipeline.json
import geotrellis.spark.pipeline.json.PipelineKeyIndexMethod
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutScheme}
import geotrellis.spark.testkit._
import geotrellis.vector.ProjectedExtent
import org.apache.spark.rdd.RDD
import org.scalatest._

class EvalSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with TestEnvironment {

  describe("Build AST") {

    case class NewReproject(`type`: String, args: List[String]) extends json.PipelineExpr

    it("should validate AST 1") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
      val read = HadoopRead(json.read.SpatialHadoop("/"))
      val tiled = TileToLayout(read, json.transform.TileToLayout())
      val reproject = BufferedReproject(tiled, json.transform.BufferedReproject("", scheme))
      //val reprojectn = NewReprojectTransform(tiled, NewReproject("id", List()))

      val write1 = HadoopWrite(reproject, json.write.Hadoop("write1", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write2 = HadoopWrite(reproject, json.write.Hadoop("write2", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write3 = HadoopWrite(reproject, null)

      write1.eval
      write2.eval
      write3.eval

      //val write1n = HadoopWrite(reprojectn, json.write.Hadoop("write1n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      //val write2n = HadoopWrite(write1n, json.write.Hadoop("write2n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      //val write3n = HadoopWrite(write2n, null)

    }

    it("should validate AST 2") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](FloatingLayoutScheme(512))
      val read = HadoopRead(json.read.SpatialHadoop("/"))
      val reproject: Node[RDD[(ProjectedExtent, Tile)] => RDD[(ProjectedExtent, Tile)]] =
        PerTileReproject(read, json.transform.PerTileReproject("", scheme))
      val tile: Node[RDD[(ProjectedExtent, Tile)] => (Int, TileLayerRDD[SpatialKey])] = TileToLayoutWithZoom(reproject, json.transform.TileToLayoutWithZoom(scheme = scheme))

      val write1 = HadoopWritePerTile(tile, json.write.Hadoop("write1", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write2 = HadoopWritePerTile(tile, json.write.Hadoop("write2", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write3 = HadoopWritePerTile(tile, null)

      write1.eval
      write2.eval
      write3.eval

      //val write1n = HadoopWrite(reprojectn, json.write.Hadoop("write1n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      //val write2n = HadoopWrite(write1n, json.write.Hadoop("write2n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      //val write3n = HadoopWrite(write2n, null)

    }
  }
}


object var2 {
  trait Node[A]
  trait Read[A] extends Node[A]
  trait Transform[A, B] extends Node[B]
  trait Write[A] extends Node[A]

  def eval[A]: Node[A] => A = { node => node match {
    case smth => eval(smth)
    case smth2 => smth2.asInstanceOf[A]
    //case smth3 => eval(smth3: Node[B]) ???
  } }
}