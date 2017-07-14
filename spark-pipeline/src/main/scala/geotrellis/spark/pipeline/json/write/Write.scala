package geotrellis.spark.pipeline.json.write

import io.circe.generic.extras.ConfiguredJsonCodec

import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.spark._
import geotrellis.spark.io.LayerWriter
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.pipeline.json._
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.util.{Component, GetComponent}

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait Write extends PipelineExpr {
  val name: String
  val profile: Option[String]
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
  val keyIndexMethod: PipelineKeyIndexMethod
  val scheme: Either[LayoutScheme, LayoutDefinition]

  def writer = LayerWriter(uri)

  def eval[
    K: SpatialComponent : AvroRecordCodec : JsonFormat : ClassTag,
    V <: CellGrid : AvroRecordCodec : ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]: JsonFormat : GetComponent[?, Bounds[K]]
  ](tuple: (Int, RDD[(K, V)] with Metadata[M])): (Int, RDD[(K, V)] with Metadata[M]) = {
    lazy val _writer = writer
    def savePyramid(zoom: Int, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
      scheme match {
        case Left(s) =>
          if (pyramid && zoom >= 1) {
            val (nextLevel, nextRdd) = Pyramid.up(rdd, s, zoom)
            savePyramid(nextLevel, nextRdd)
          }
        case Right(_) =>
          if (pyramid)
            logger.error("Pyramiding only supported with layoutScheme, skipping pyramid step")
      }
    }

    val (zoom, rdd) = tuple
    _writer.write(LayerId(name, zoom), rdd, keyIndexMethod.getKeyIndexMethod[K])
    savePyramid(zoom, rdd)
    tuple
  }
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
