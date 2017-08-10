package geotrellis.spark.pipeline.ast.untyped

import geotrellis.spark.pipeline.ast.Node
import geotrellis.spark.pipeline.json._

import cats.implicits._
import io.circe
import io.circe.parser.decode

import scala.reflect.runtime.universe.TypeTag

object Implicits extends Implicits

trait Implicits {
  implicit class withPipelineExpressionExtensions(list: List[PipelineExpr]) {
    def typeErased: ErasedNode = ErasedUtils.fromPipelineExprList(list)
    def get[T <: Node[_]: TypeTag]: T = typeErased.get[T]
  }

  implicit class withStringExtentions(json: String) {
    def toPipelineExpr: Either[circe.Error, List[PipelineExpr]] = decode[List[PipelineExpr]](json)
    def toTypeErased: Option[ErasedNode] = toPipelineExpr.map(_.typeErased).toOption
    def toTyped[T <: Node[_]: TypeTag]: Option[T] = toTypeErased.map(_.get[T])
  }
}
