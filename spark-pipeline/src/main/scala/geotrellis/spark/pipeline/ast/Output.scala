/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.write.{Write => JsonWrite}
import geotrellis.raster.CellGrid
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.spark._
import geotrellis.spark.io.LayerWriter
import geotrellis.layers.io.avro.AvroRecordCodec
import geotrellis.tiling.{Bounds, LayoutDefinition, SpatialComponent}
import geotrellis.util.{Component, GetComponent}
import com.typesafe.scalalogging.LazyLogging
import geotrellis.layers.{LayerId, Metadata}
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait Output[T] extends Node[T]

object Output extends LazyLogging {
  def write[
    K: SpatialComponent : AvroRecordCodec : JsonFormat : ClassTag,
    V <: CellGrid[Int] : AvroRecordCodec : ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
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
