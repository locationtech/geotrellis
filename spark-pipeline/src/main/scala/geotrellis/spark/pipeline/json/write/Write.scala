package geotrellis.spark.pipeline.json.write

import geotrellis.spark.pipeline.json._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}

import io.circe.generic.extras.ConfiguredJsonCodec

trait Write extends PipelineExpr {
  val name: String
  val profile: Option[String]
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
  val keyIndexMethod: PipelineKeyIndexMethod
  val scheme: Either[LayoutScheme, LayoutDefinition]
}

@ConfiguredJsonCodec
case class File(
  name: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None,
  profile: Option[String] = None,
  `type`: String = "write.file"
) extends Write

@ConfiguredJsonCodec
case class Hadoop(
  name: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None,
  profile: Option[String] = None,
  `type`: String = "write.hadoop"
) extends Write

@ConfiguredJsonCodec
case class S3(
  name: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None,
  profile: Option[String] = None,
  `type`: String = "write.s3"
) extends Write

@ConfiguredJsonCodec
case class Accumulo(
  name: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None,
  profile: Option[String] = None,
  `type`: String = "write.accumulo"
) extends Write

@ConfiguredJsonCodec
case class Cassandra(
  name: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None,
  profile: Option[String] = None,
  `type`: String = "write.cassandra"
) extends Write

@ConfiguredJsonCodec
case class HBase(
  name: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None,
  profile: Option[String] = None,
  `type`: String = "write.hbase"
) extends Write
