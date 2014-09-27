/*
 * Copyright (c) 2014 DigitalGlobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.rdd

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.op.local._

import org.apache.spark.Partition
import org.apache.spark.SparkContext
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD

class RasterRDD(val prev: RDD[TmsTile], val metaData: LayerMetaData)
    extends RDD[TmsTile](prev) {

  override val partitioner = prev.partitioner

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

  def mapTiles(f: TmsTile => TmsTile): RasterRDD =
    asRasterRDD(metaData) {
      mapPartitions({ partition =>
        partition.map { tile =>
          f(tile)
        }
      }, true)
    }

  def combineTiles(other: RasterRDD)(f: (TmsTile, TmsTile) => TmsTile): RasterRDD =
    asRasterRDD(metaData) {
      zipPartitions(other, true) { (partition1, partition2) =>
        partition1.zip(partition2).map {
          case (tile1, tile2) =>
            f(tile1, tile2)
        }
      }
    }

  def combineTiles(others: Seq[RasterRDD]): RDD[Seq[TmsTile]] = {
    def recurse(tail: RDD[Seq[TmsTile]],
      rasters: List[RasterRDD]): RDD[Seq[TmsTile]] = {

    }
  }

  def combineTiles(others: Seq[RasterRDD])(f: (Seq[TmsTile]) => TmsTile): RasterRDD = {
    def recurse(tail: RasterRDD, rasters: List[RasterRDD]): RasterRDD = rasters match {
      case Nil => tail
      case x :: xs =>
        val rs = asRasterRDD(metaData) {
          tail.zipPartitions(x, true) { (partition1, partition2) =>
            partition1.zip(partition2).map {
              case (tile1, tile2) =>
                f(Seq(tile1, tile2))
            }
          }
        }

        recurse(rs, xs)
    }

    recurse(this, others.toList)
  }

  def minMax: (Int, Int) =
    map(_.tile.findMinMax)
      .reduce { (t1, t2) =>
      val (min1, max1) = t1
      val (min2, max2) = t2
      val min =
        if(isNoData(min1)) min2
        else {
          if(isNoData(min2)) min1
          else math.min(min1, min2)
        }
      val max =
        if(isNoData(max1)) max2
        else {
          if(isNoData(max2)) max1
          else math.max(max1, max2)
        }
      (min, max)
    }
}
