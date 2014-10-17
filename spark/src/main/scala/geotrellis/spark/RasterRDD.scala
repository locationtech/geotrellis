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

package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.ingest._
import geotrellis.vector.Extent
import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

class RasterRDD[K: ClassTag](val tileRdd: RDD[(K, Tile)], val metaData: RasterMetaData) extends RDD[(K, Tile)](tileRdd) {
  override val partitioner = tileRdd.partitioner

  override def getPartitions: Array[Partition] = firstParent.partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent.iterator(split, context)

  def mapTiles(f: ((K, Tile)) => (K, Tile)): RasterRDD[K] =
    asRasterRDD(metaData) {
      mapPartitions({ partition =>
        partition.map { tile =>
          f(tile)
        }
      }, true)
    }

  def combineTiles[R: ClassTag](other: RasterRDD[K])(f: ((K, Tile), (K, Tile)) => (R, Tile)): RasterRDD[R] =
    asRasterRDD(metaData) {
      zipPartitions(other, true) { (partition1, partition2) =>
        partition1.zip(partition2).map {
          case (tile1, tile2) =>
            f(tile1, tile2)
        }
      }
    }

  def combineTiles(others: Seq[RasterRDD[K]])(f: (Seq[(K, Tile)] => (K, Tile))): RasterRDD[K] = {
    def create(t: (K, Tile)) = Seq(t)
    def mergeValue(ts: Seq[(K, Tile)], t: (K, Tile)) = ts :+ t
    def mergeContainers(ts1: Seq[(K, Tile)], ts2: Seq[(K, Tile)]) = ts1 ++ ts2

    asRasterRDD(metaData) {
      (this :: others.toList)
        .map(_.tileRdd)
        .reduceLeft(_ ++ _)
        .map(t => (t.id, t))
        .combineByKey(create, mergeValue, mergeContainers)
        .map { case (id, tiles) => f(tiles) }
    }
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
