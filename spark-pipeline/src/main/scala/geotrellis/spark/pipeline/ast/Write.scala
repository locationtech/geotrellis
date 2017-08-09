package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.write.{Write => JsonWrite}
import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.spark._
import geotrellis.spark.io.LayerWriter
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.pipeline.json._
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util.{Component, GetComponent, LazyLogging}

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait Write[T] extends Node[T]

object Write extends LazyLogging {
  def eval[
    K: SpatialComponent : AvroRecordCodec : JsonFormat : ClassTag,
    V <: CellGrid : AvroRecordCodec : ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]: JsonFormat : GetComponent[?, Bounds[K]]
  ](arg: JsonWrite)(tuples: Stream[(Int, RDD[(K, V)] with Metadata[M])]): Stream[(Int, RDD[(K, V)] with Metadata[M])] = {
    lazy val writer = LayerWriter(arg.uri)
    def savePyramid(zoom: Int, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
      arg.scheme match {
        case Left(s) =>
          if (arg.pyramid && zoom >= 1) {
            val (nextLevel, nextRdd) = Pyramid.up(rdd, s, zoom)
            savePyramid(nextLevel, nextRdd)
          }
        case Right(_) =>
          if (arg.pyramid)
            logger.error("Pyramiding only supported with layoutScheme, skipping pyramid step")
      }
    }

    tuples.foreach { tuple =>
      val (zoom, rdd) = tuple
      writer.write(LayerId(arg.name, zoom), rdd, arg.keyIndexMethod.getKeyIndexMethod[K])
      savePyramid(zoom, rdd)
    }

    tuples
  }

}
