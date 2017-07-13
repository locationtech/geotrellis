package geotrellis.spark.pipeline.ast

import io.circe.syntax._
import geotrellis.spark._
import geotrellis.spark.pipeline._
import geotrellis.spark.pipeline.json
import geotrellis.spark.pipeline.json.{PipelineExpr, PipelineKeyIndexMethod}
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.spark.testkit._
import org.apache.spark.SparkContext
import org.scalatest._

class AstSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with TestEnvironment {

  describe("Build AST") {
    case class NewReproject(`type`: String, args: List[String]) extends PipelineExpr

    case class NewReprojectTransform(
      node: Node[TileLayerRDD[SpatialKey]],
      arg: NewReproject
    ) extends Transform[TileLayerRDD[SpatialKey], (Int, TileLayerRDD[SpatialKey])] {
      def get(implicit sc: SparkContext): (Int, TileLayerRDD[SpatialKey]) = {
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
    }

    it("should validate AST") {
      import singleband.spatial._
      val scheme = Left[LayoutScheme, LayoutDefinition](null)
      val read = HadoopRead(json.read.SpatialHadoop("/"))
      val tiled = TileToLayout(read, json.transform.TileToLayout())
      val reproject = BufferedReproject(tiled, json.transform.BufferedReproject("", scheme))
      val reprojectn = NewReprojectTransform(tiled, NewReproject("id", List()))

      val write1 = HadoopWrite(reproject, json.write.Hadoop("write1", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write2 = HadoopWrite(write1, json.write.Hadoop("write2", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write3 = HadoopWrite(write2, null)
      val write1n = HadoopWrite(reprojectn, json.write.Hadoop("write1n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write2n = HadoopWrite(write1n, json.write.Hadoop("write2n", "/tmp", pyramid = true, PipelineKeyIndexMethod("zorder"), scheme))
      val write3n = HadoopWrite(write2n, null)

      /*
      println
      println
      println(write1)
      println
      println(write2)
      println
      println(write3.validation)
      println
      println
      println(write1n)
      println
      println(write2n)
      println
      println(write3n.validation)
      println
      println
      */

      println
      println
      println(write1.asJson)
      println
      println(write2.asJson)
      println
      println(write3.validation)
      println
      println
      println(write1n.asJson)
      println
      println(write2n.asJson)
      println
      println(write3n.validation)
      println
      println
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