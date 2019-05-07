/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.buffer

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._
import geotrellis.tiling.{SpatialComponent, SpatialKey}
import geotrellis.util._

import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer

import Direction._

object CollectNeighbors {

  /** Collects the neighbors of each value (including itself) into a Map
    * giving the direction of the neighbor.
    */
  def apply[K: SpatialComponent: ClassTag, V](rdd: RDD[(K, V)]): RDD[(K, Iterable[(Direction, (K, V))])] = {
    val neighbored: RDD[(K, (Direction, (K, V)))] =
      rdd
        .flatMap { case (key, value) =>
          val SpatialKey(col, row) = key

          Seq(
            (key, (Center, (key, value))),

            (key.setComponent(SpatialKey(col-1, row)), (Right, (key, value))),
            (key.setComponent(SpatialKey(col+1, row)), (Left, (key, value))),
            (key.setComponent(SpatialKey(col, row-1)), (Bottom, (key, value))),
            (key.setComponent(SpatialKey(col, row+1)), (Top, (key, value))),

            (key.setComponent(SpatialKey(col-1, row-1)), (BottomRight, (key, value))),
            (key.setComponent(SpatialKey(col+1, row-1)), (BottomLeft, (key, value))),
            (key.setComponent(SpatialKey(col-1, row+1)), (TopRight, (key, value))),
            (key.setComponent(SpatialKey(col+1, row+1)), (TopLeft, (key, value)))
          )
        }

    val grouped: RDD[(K, Iterable[(Direction, (K, V))])] =
      rdd.partitioner match {
        case Some(partitioner) => neighbored.groupByKey(partitioner)
        case None => neighbored.groupByKey
      }

    grouped
      .filter { case (_, values) =>
        values.find {
          case (Center, _) => true
          case _ => false
        }.isDefined
      }
  }
}
