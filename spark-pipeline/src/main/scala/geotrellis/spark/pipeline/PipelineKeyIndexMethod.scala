package geotrellis.spark.pipeline

case class PipelineKeyIndexMethod(
  `type`: String,
  timeTag: Option[String] = None,
  timeFormat: Option[String] = None,
  temporalResolution: Option[Int] = None
)