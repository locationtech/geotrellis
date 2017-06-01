package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.spark.testkit._
import org.scalatest._

class AstSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with TestEnvironment {

  describe("Build AST") {
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

      val write1 = HadoopWrite(reproject, json.WriteHadoop("", "", "", "/tmp", true, null))

      val write2 = HadoopWrite(write1, json.WriteHadoop("", "", "", "/tmp", true, null))

      println(write1)
      println(write2)


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