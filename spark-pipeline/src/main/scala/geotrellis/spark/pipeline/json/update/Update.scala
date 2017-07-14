package geotrellis.spark.pipeline.json.update

import geotrellis.spark.pipeline.json._
import io.circe.generic.extras.ConfiguredJsonCodec

trait Update extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
}

@ConfiguredJsonCodec
case class File(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.file"
) extends Update

@ConfiguredJsonCodec
case class Hadoop(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.hadoop"
) extends Update

@ConfiguredJsonCodec
case class S3(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.s3"
) extends Update

@ConfiguredJsonCodec
case class Accumulo(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.accumulo"
) extends Update

@ConfiguredJsonCodec
case class Cassandra(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.cassandra"
) extends Update

@ConfiguredJsonCodec
case class HBase(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  maxZoom: Option[Int] = None,
  `type`: String = "update.hbase"
) extends Update
