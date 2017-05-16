package geotrellis.spark.pipeline

trait Write extends PipelineExpr {
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
)

case class ReindexS3(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.s3"
)

case class ReindexAccumulo(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.accumulo"
)

case class ReindexCassandra(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.cassandra"
)

case class ReindexHBase(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.hbase"
)
