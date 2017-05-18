package geotrellis.spark.pipeline.json

trait Update extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
}

case class UpdateFile(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None
) extends Update

case class UpdateHadoop(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None
) extends Update

case class UpdateS3(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None
) extends Update

case class UpdateAccumulo(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None
) extends Update

case class UpdateCassandra(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None
) extends Update

case class UpdateHBase(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None
) extends Update
