package geotrellis.spark.pipeline.ast.untyped

import geotrellis.spark.pipeline.ast._
import geotrellis.spark.pipeline.json.PipelineExpr
import org.apache.spark.SparkContext

object Test {
  import singleband.spatial._

  val ast: Node[Any] = ???

  /**
    * We have not a lot of types
    * Temporal
    * Spatial
    * Singleband
    * Multiband
    *
    * Tiled (RDD with Metadata)
    * Before tiling (RDD)
    *
    * */

  /*case class UntypedNode(expr: PipelineExpr) {
    def deriveType = expr match {
      case json.
    }
  }

  val json: List[PipelineExpr] = ???

  val untypedAST: List[UntypedNode] = json.map(UntypedNode)

  def typeCheck(node: Node[Any])(implicit sc: SparkContext) = {
    ast match {
      case n => n.`type`
    }
  }*/

}
