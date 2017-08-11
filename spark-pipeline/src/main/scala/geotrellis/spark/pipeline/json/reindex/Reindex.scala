package geotrellis.spark.pipeline.json.reindex

import geotrellis.spark.pipeline.json._
import io.circe.generic.extras.ConfiguredJsonCodec

// TODO: implement node for these PipelineExpr
trait Reindex extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
}

@ConfiguredJsonCodec
case class JsonReindex(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: PipelineExprType
) extends Reindex
