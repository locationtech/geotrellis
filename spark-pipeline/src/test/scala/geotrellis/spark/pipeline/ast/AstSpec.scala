package geotrellis.spark.pipeline.ast

import geotrellis.spark.{MultibandTileLayerRDD, SpaceTimeKey, SpatialKey, TileLayerRDD}
import geotrellis.spark.pipeline.json
import geotrellis.spark.pipeline.json.PipelineExpr
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.spark.testkit._
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
      def get: (Int, TileLayerRDD[SpatialKey]) = {
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
      val read = HadoopRead(
        json.SpatialReadHadoop("/", "", "")
      )

      val tiled = TileToLayout(
        read, json.TransformTile()
      )

      val reproject = BufferedReproject(
        tiled, json.TransformBufferedReproject("", "", Left[LayoutScheme, LayoutDefinition](null))
      )

      val reprojectn = NewReprojectTransform(
        tiled, NewReproject("id", List())
      )

      val write1 = HadoopWrite(reproject, json.WriteHadoop("", "", "", "/tmp", true, null))

      val write2 = HadoopWrite(write1, json.WriteHadoop("", "", "", "/tmp", true, null))

      val write3 = HadoopWrite(write2, null)

      val write1n = HadoopWrite(reprojectn, json.WriteHadoop("", "", "", "/tmp", true, null))

      val write2n = HadoopWrite(write1n, json.WriteHadoop("", "", "", "/tmp", true, null))

      val write3n = HadoopWrite(write2n, null)

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
    }
  }

}


object var2 {
  trait Node[A]
  trait Read[A] extends Node[A]
  trait Transform[A, B] extends Node[B]
  trait Write[A] extends Node[A]

  def eval[A]: Node[A] => A = ???
}