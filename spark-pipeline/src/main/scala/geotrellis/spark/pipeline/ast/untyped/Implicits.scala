package geotrellis.spark.pipeline.ast.untyped

import geotrellis.spark.pipeline.ast.Node
import geotrellis.spark.pipeline.json.PipelineExpr

import scala.reflect.runtime.universe.TypeTag

object Implicits extends Implicits

trait Implicits {
  implicit class withPipelineExpressionExtensions(list: List[PipelineExpr]) {
    def typeErased = ErasedUtils.fromPipelineExprList(list)
    def get[T <: Node[_]: TypeTag]: T = typeErased.get[T]
  }
}
