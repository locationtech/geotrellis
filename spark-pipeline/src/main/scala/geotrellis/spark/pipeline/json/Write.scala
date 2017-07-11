package geotrellis.spark.pipeline.json

import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.spark._
import geotrellis.spark.io.LayerWriter
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.util.{Component, GetComponent}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait Write extends PipelineExpr {
  val name: String
  val profile: String
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
  ](tuple: (Int, RDD[(K, V)] with Metadata[M])) = {
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
            println("Pyramiding only supported with layoutScheme, skipping pyramid step")
      }
    }

    val (zoom, rdd) = tuple
    _writer.write(LayerId(name, zoom), rdd, keyIndexMethod.getKeyIndexMethod[K])
    savePyramid(zoom, rdd)
    tuple
  }
}

case class WriteFile(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write

case class WriteHadoop(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
)(implicit sc: SparkContext) extends Write

case class WriteS3(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write

case class WriteAccumulo(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write

case class WriteCassandra(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write

case class WriteHBase(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write
