package geotrellis.spark.pipeline.json

import geotrellis.proj4.CRS

import scala.util.Try
import java.net.URI

trait Read extends PipelineExpr {
  val profile: String
  val uri: String
  val crs: Option[String]
  val tag: Option[String]
  val maxTileSize: Option[Int]
  val partitions: Option[Int]
  val clip: Boolean

  def getURI = new URI(uri)
  def getCRS = crs.map(c => Try(CRS.fromName(c)) getOrElse CRS.fromString(c))
  def getTag = tag.getOrElse("default")
}

case class ReadHadoop(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
) extends Read

case class ReadS3(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
) extends Read

case class ReadFile(
  `type`: String,
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false
) extends Read
