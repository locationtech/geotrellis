package geotrellis.spark.pipeline

trait Update extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
  val `type`: String
}

case class UpdateFile(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.file"
) extends Update

case class UpdateHadoop(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.hadoop"
) extends Update

case class UpdateS3(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.s3"
) extends Update

case class UpdateAccumulo(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.accumulo"
) extends Update

case class UpdateCassandra(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.cassandra"
) extends Update

case class UpdateHBase(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.hbase"
) extends Update
