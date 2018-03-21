package geotrellis.spark.pipeline.json.write

import geotrellis.spark.pipeline.json._
import geotrellis.tiling.{LayoutDefinition, LayoutScheme}

import io.circe.generic.extras.ConfiguredJsonCodec

trait Write extends PipelineExpr {
  val name: String
  val profile: Option[String]
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
  val scheme: Either[LayoutScheme, LayoutDefinition]
}

@ConfiguredJsonCodec
case class JsonWrite(
  name: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  profile: Option[String] = None,
  `type`: PipelineExprType
) extends Write
