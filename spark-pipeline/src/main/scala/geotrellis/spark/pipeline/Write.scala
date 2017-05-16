package geotrellis.spark.pipeline

import geotrellis.spark.TileLayerRDD

trait Write extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
  val keyIndexMethod: PipelineKeyIndexMethod
}
case class WriteFile(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None,
  `type`: String = "write.file"
) extends Write

case class WriteHadoop(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None,
  `type`: String = "write.hadoop"
) extends Write

case class WriteS3(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None,
  `type`: String = "write.s3"
) extends Write

case class WriteAccumulo(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None,
  `type`: String = "write.accumulo"
) extends Write

case class WriteCassandra(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None,
  `type`: String = "write.cassandra"
) extends Write

case class WriteHBase(
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None,
  `type`: String = "write.hbase"
) extends Write
