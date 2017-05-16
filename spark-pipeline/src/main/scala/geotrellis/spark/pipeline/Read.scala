package geotrellis.spark.pipeline

import java.net.URI

import geotrellis.proj4.CRS
import geotrellis.raster.CellGrid
import geotrellis.spark.Metadata
import geotrellis.util.Component
import geotrellis.vector.ProjectedExtent
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._
import scala.util.Try

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

  def eval[I, V]: RDD[(I, V)]
}

case class ReadHadoop(
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false,
  `type`: String = "read.hadoop"
) extends Read {
  def eval[I, V]: RDD[(I, V)] = {
    null
  }
}

case class ReadS3(
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false,
  `type`: String = "read.s3"
) extends Read {
  def eval[I, V]: RDD[(I, V)] = {
    null
  }
}

case class ReadFile(
  profile: String,
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  clip: Boolean = false,
  `type`: String = "read.file"
) extends Read {
  def eval[I, V]: RDD[(I, V)] = {
    null
  }
}
