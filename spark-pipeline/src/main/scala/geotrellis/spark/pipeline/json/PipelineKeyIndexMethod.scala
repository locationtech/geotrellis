package geotrellis.spark.pipeline.json

case class PipelineKeyIndexMethod(
  `type`: String,
  timeTag: Option[String] = None,
  timeFormat: Option[String] = None,
  temporalResolution: Option[Int] = None
)