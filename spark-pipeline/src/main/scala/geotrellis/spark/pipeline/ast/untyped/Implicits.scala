package geotrellis.spark.pipeline.ast.untyped

import geotrellis.spark.pipeline.ast.Node
import geotrellis.spark.pipeline.json._

import cats.implicits._
import io.circe
import io.circe.parser.decode
import org.apache.spark.SparkContext

import scala.reflect.runtime.universe.TypeTag

object Implicits extends Implicits

trait Implicits {
  implicit class withPipelineExpressionExtensions(list: List[PipelineExpr]) {
    def erasedNode: ErasedNode = ErasedUtils.fromPipelineExprList(list)
    def node[T <: Node[_]: TypeTag]: T = erasedNode.node[T]
    def unsafeRun(implicit sc: SparkContext): Any = erasedNode.unsafeRun
    def run[T: TypeTag](implicit sc: SparkContext): T = erasedNode.run[T]
  }

  implicit class withStringExtentions(json: String) {
    def pipelineExpr: Either[circe.Error, List[PipelineExpr]] = decode[List[PipelineExpr]](json)
    def erasedNode: Option[ErasedNode] = pipelineExpr.map(_.erasedNode).toOption
    def node[T <: Node[_]: TypeTag]: Option[T] = erasedNode.map(_.node[T])
    def unsafeRun(implicit sc: SparkContext): Option[Any] = erasedNode.map(_.unsafeRun)
    def run[T: TypeTag](implicit sc: SparkContext): Option[T] = erasedNode.map(_.run[T])
  }
}
