package geotrellis.spark.pipeline.json.reindex

import geotrellis.spark.pipeline.json._
import io.circe.generic.extras.ConfiguredJsonCodec

trait Reindex extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val keyIndexMethod: PipelineKeyIndexMethod
}

@ConfiguredJsonCodec
case class Hadoop(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.hadoop"
) extends Reindex

@ConfiguredJsonCodec
case class S3(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.s3"
) extends Reindex

@ConfiguredJsonCodec
case class Accumulo(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.accumulo"
) extends Reindex

@ConfiguredJsonCodec
case class Cassandra(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.cassandra"
) extends Reindex

@ConfiguredJsonCodec
case class HBase(
  name: String,
  profile: String,
  uri: String,
  keyIndexMethod: PipelineKeyIndexMethod,
  `type`: String = "reindex.hbase"
) extends Reindex
