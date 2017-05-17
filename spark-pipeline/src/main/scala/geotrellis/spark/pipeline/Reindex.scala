package geotrellis.spark.pipeline

trait Reindex extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
}

case class ReindexHadoop(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.hadoop"
) extends Reindex

case class ReindexS3(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.s3"
) extends Reindex

case class ReindexAccumulo(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.accumulo"
) extends Reindex

case class ReindexCassandra(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.cassandra"
) extends Reindex

case class ReindexHBase(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.hbase"
) extends Reindex
