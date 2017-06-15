package geotrellis.spark.pipeline.json

import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.spark.{Bounds, LayerId, Metadata, SpatialComponent, TileLayerMetadata}
import geotrellis.spark.io.LayerWriter
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.file.FileLayerWriter
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.util.{Component, GetComponent}
import org.apache.hadoop.fs.Path
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

  def writer: LayerWriter[LayerId]

  def eval[
    K: SpatialComponent : AvroRecordCodec : JsonFormat : ClassTag,
    V <: CellGrid : AvroRecordCodec : ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]: JsonFormat : GetComponent[?, Bounds[K]]
  ](tuple: (Int, RDD[(K, V)] with Metadata[M])) = {
    def savePyramid(zoom: Int, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
      val currentId = LayerId(name, zoom)

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
    writer.write(LayerId(name, zoom), rdd, keyIndexMethod.getKeyIndexMethod[K])
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
) extends Write {
  lazy val writer = FileLayerWriter(uri)
}

case class WriteHadoop(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
)(implicit sc: SparkContext) extends Write {
  lazy val writer = HadoopLayerWriter(new Path(uri))
}

case class WriteS3(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}

case class WriteAccumulo(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}

case class WriteCassandra(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}

case class WriteHBase(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  scheme: Either[LayoutScheme, LayoutDefinition],
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}
