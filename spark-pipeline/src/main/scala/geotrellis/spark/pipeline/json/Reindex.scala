package geotrellis.spark.pipeline.json

trait Reindex extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
}

case class ReindexHadoop(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod
) extends Reindex

case class ReindexS3(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod
) extends Reindex

case class ReindexAccumulo(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod
) extends Reindex

case class ReindexCassandra(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod
) extends Reindex

case class ReindexHBase(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod
) extends Reindex
