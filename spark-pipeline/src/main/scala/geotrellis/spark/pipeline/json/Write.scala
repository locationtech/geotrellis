package geotrellis.spark.pipeline.json

trait Write extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
  val keyIndexMethod: PipelineKeyIndexMethod
}
case class WriteFile(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write

case class WriteHadoop(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write

case class WriteS3(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write

case class WriteAccumulo(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write

case class WriteCassandra(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write

case class WriteHBase(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write
