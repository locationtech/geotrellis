package geotrellis.spark.pipeline.json.read

import geotrellis.spark.io.hadoop.HadoopGeoTiffRDD
import geotrellis.spark.pipeline.json._

import io.circe.generic.extras.ConfiguredJsonCodec

import java.net.URI

trait Read extends PipelineExpr {
  val uri: String
  val crs: Option[String]
  val tag: Option[String]
  val maxTileSize: Option[Int]
  val partitions: Option[Int]

  def getURI = new URI(uri)
  def getTag = tag.getOrElse("default")
}

@ConfiguredJsonCodec
case class JsonRead(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  partitionBytes: Option[Long] = None,
  chunkSize: Option[Int] = None,
  delimiter: Option[String] = None,
  timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
  timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
  `type`: PipelineExprType
) extends Read

