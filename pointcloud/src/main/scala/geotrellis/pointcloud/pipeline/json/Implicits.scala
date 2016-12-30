package geotrellis.pointcloud.pipeline.json

import geotrellis.pointcloud.pipeline._

import io.circe.{Encoder, Json}
import io.circe.generic.extras._
import io.circe.generic.extras.auto._
import io.circe.syntax._

object Implicits extends Implicits

trait Implicits {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseKeys.withDiscriminator("class_type")

  implicit def exprTypeEncoder[T <: ExprType]: Encoder[T] = Encoder.instance { _.toString.asJson }
  implicit val rawExprEncoder: Encoder[RawExpr] = Encoder.instance { _.json }
  implicit val pipelineConstructorEncoder: Encoder[PipelineConstructor] = Encoder.instance { constructor =>
    Json.obj(
      "pipeline" -> constructor.list
        .map(
          _.asJsonObject
            .remove("class_type") // remove type
            .filter { case (key, value) => !value.isNull } // cleanup options
        ).asJson
    )
  }
}
