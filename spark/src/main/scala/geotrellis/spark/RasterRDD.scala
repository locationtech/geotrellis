/*
 * Copyright (c) 2015 Azavea.
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
import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

class RasterRDD[K: ClassTag, T: ClassTag](val tileRdd: RDD[(K, T)], val metaData: RasterMetaData) extends RDD[(K, T)](tileRdd) {
  type Self = RasterRDD[K, T]

  def wrap(f: => RDD[(K, T)]): Self =
    new RasterRDD[K, T](f, metaData) 
 
  override val partitioner = tileRdd.partitioner

  override def getPartitions: Array[Partition] = firstParent[(K, Tile)].partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent[(K, T)].iterator(split, context)  

  def reduceByKey(f: (T, T) => T): Self = wrap { 
    tileRdd.reduceByKey(f) 
  }

  def mapKeys[R: ClassTag](f: K => K): Self = wrap {
    tileRdd map { case (key, tile) => f(key) -> tile }
  }

  def mapTiles(f: T => T): Self = wrap {
    tileRdd map { case (key, tile) => key -> f(tile) }
  }

  def mapPairs(f: ((K, T)) => (K, T)): Self = wrap {
    tileRdd map { row => f(row) }
  }

  def combinePairs(other: Self)(f: ((K, T), (K, T)) => (K, T)): Self = wrap {
    zipPartitions(other, true) { (partition1, partition2) =>
      partition1.zip(partition2) map { case (row1, row2) => f(row1, row2) }
    }    
  }

  def combineTiles(other: Self)(f: (T, T) => T): Self =
    combinePairs(other) { case ((k1, t1), (k2, t2)) => (k1, f(t1, t2)) }

  def combinePairs(others: Traversable[Self])(f: (Traversable[(K, T)] => (K, T))): Self = {
    def create(t: (K, T)) = Vector(t)
    def mergeValue(ts: Vector[(K, T)], t: (K, T)) = ts :+ t
    def mergeContainers(ts1: Vector[(K, T)], ts2: Traversable[(K, T)]) = ts1 ++ ts2

    wrap {
      (this :: others.toList)
        .map(_.tileRdd)
        .reduceLeft(_ ++ _)
        .map(t => (t._1, t))
        .combineByKey(create, mergeValue, mergeContainers)
        .map { case (id, tiles) => f(tiles) }
    }
  }
}

object RasterRDD {
  implicit class SpatialRasterRDD(val rdd: RasterRDD[SpatialKey, Tile]) extends SpatialRasterRDDMethods
}