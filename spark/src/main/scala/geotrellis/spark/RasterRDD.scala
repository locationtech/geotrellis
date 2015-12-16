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
import geotrellis.spark.io.ContainerConstructor
import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.reflect.ClassTag

class RasterRDD[K: ClassTag](val tileRdd: RDD[(K, Tile)], val metaData: RasterMetaData)
  extends BoundRDD[K, Tile](tileRdd) {
  override val partitioner = tileRdd.partitioner

  override def getPartitions: Array[Partition] = firstParent[(K, Tile)].partitions

  override def compute(split: Partition, context: TaskContext) =
    firstParent[(K, Tile)].iterator(split, context)

  def convert(cellType: CellType): RasterRDD[K] =
    mapTiles(_.convert(cellType))

  def reduceByKey(f: (Tile, Tile) => Tile): RasterRDD[K] =
    asRasterRDD(metaData) { tileRdd.reduceByKey(f) }


  def mapKeys[R: ClassTag](f: K => R): RasterRDD[R] =
    asRasterRDD(metaData) {
      tileRdd map { case (key, tile) => f(key) -> tile }
    }

  def mapTiles(f: Tile => Tile): RasterRDD[K] =
    asRasterRDD(metaData) {
      tileRdd map { case (key, tile) => key -> f(tile) }
    }

  def mapPairs[R: ClassTag](f: ((K, Tile)) => (R, Tile)): RasterRDD[R] =
    asRasterRDD(metaData) {
      tileRdd map { row => f(row) }
    }

  def combineTiles(other: RasterRDD[K])(f: (Tile, Tile) => Tile): RasterRDD[K] =
    asRasterRDD(metaData) {
      this.union(other).reduceByKey(f)
    }

  def combinePairs[R: ClassTag](other: RasterRDD[K])(f: ((K, Tile), (K, Tile)) => (R, Tile)): RasterRDD[R] =
    asRasterRDD(metaData) {
      this.join(other).map { case (key, (tile1, tile2)) => f((key, tile1), (key, tile2)) }
    }

  def combinePairs(others: Traversable[RasterRDD[K]])(f: (Traversable[(K, Tile)] => (K, Tile))): RasterRDD[K] = {
    def create(t: (K, Tile)) = List(t)
    def mergeValue(ts: List[(K, Tile)], t: (K, Tile)) = ts :+ t
    def mergeContainers(ts1: List[(K, Tile)], ts2: Traversable[(K, Tile)]) = ts1 ++ ts2

    asRasterRDD(metaData) {
      (this :: others.toList)
        .map(_.tileRdd)
        .reduceLeft(_ ++ _)
        .map(t => (t.id, t))
        .combineByKey(create, mergeValue, mergeContainers)
        .map { case (id, tiles) => f(tiles) }
    }
  }

  def asRasters()(implicit sc: SpatialComponent[K]): RDD[(K, Raster)] =
    mapPartitions({ part =>
      part.map { case (key, tile) =>
        (key, Raster(tile, metaData.mapTransform(key)))
      }
    }, true)

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

  def minMaxDouble: (Double, Double) =
    map(_.tile.findMinMaxDouble)
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

object RasterRDD {
  implicit class SpatialRasterRDD(val rdd: RasterRDD[SpatialKey]) extends SpatialRasterRDDMethods

  implicit def implicitToRDD[K](rasterRdd: RasterRDD[K]): RDD[(K, Tile)] = rasterRdd

  implicit def constructor[K: JsonFormat : ClassTag] =
    new ContainerConstructor[K, Tile, RasterRDD[K]] {
      type MetaDataType = RasterMetaData
      implicit def metaDataFormat = geotrellis.spark.io.json.RasterMetaDataFormat

      def getMetaData(raster: RasterRDD[K]): RasterMetaData =
        raster.metaData

      def makeContainer(rdd: RDD[(K, Tile)], bounds: KeyBounds[K], metadata: MetaDataType) =
        new RasterRDD(rdd, metadata)

      def combineMetaData(that: MetaDataType, other: MetaDataType): MetaDataType =
        that.combine(other)
    }
}
