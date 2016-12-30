package geotrellis.pointcloud

import io.circe.{Encoder, Json}
import io.circe.syntax._

package object pipeline extends json.Implicits {
  implicit def pipelineExprToJson[T <: PipelineExpr](expr: T)(implicit encoder: Encoder[T]): Json = expr.asJson
  implicit def pipelineConstructorToJson(cons: PipelineConstructor): Json = cons.asJson
  implicit def jsonToString(json: Json): String = json.toString
  implicit def pipelineExprToString[T <: PipelineExpr](expr: T)(implicit encoder: Encoder[T]): String = expr.asJson.toString
  implicit def pipelineConstructorToString(cons: PipelineConstructor): String = cons.asJson.toString
}
