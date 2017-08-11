package geotrellis.spark.pipeline.json.update

import geotrellis.spark.pipeline.json._
import io.circe.generic.extras.ConfiguredJsonCodec

// TODO: implement node for these PipelineExpr
trait Update extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
}

@ConfiguredJsonCodec
case class JsonUpdate(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: PipelineExprType
) extends Update
