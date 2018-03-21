package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.write.{Write => JsonWrite}
import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.io.LayerWriter
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.util.{Component, GetComponent, LazyLogging}

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait Output[T] extends Node[T]

object Output extends LazyLogging {
  def write[
    K: SpatialComponent : AvroRecordCodec : JsonFormat : ClassTag,
    V <: CellGrid : AvroRecordCodec : ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]: JsonFormat : GetComponent[?, Bounds[K]]
  ](arg: JsonWrite)(tuples: Stream[(Int, RDD[(K, V)] with Metadata[M])]): Stream[(Int, RDD[(K, V)] with Metadata[M])] = {
    lazy val writer = LayerWriter(arg.uri)
    tuples.foreach { tuple =>
      val (zoom, rdd) = tuple
      writer.write(LayerId(arg.name, zoom), rdd, arg.keyIndexMethod.getKeyIndexMethod[K])
    }

    tuples
  }

}
