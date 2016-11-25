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

package geotrellis.spark.mapalgebra.zonal

import geotrellis.raster.mapalgebra.zonal._
import geotrellis.raster.summary._
import geotrellis.raster.histogram._
import geotrellis.raster._
import geotrellis.spark._

import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import spire.syntax.cfor._

import scala.reflect.ClassTag

object Zonal {
  private def mergeMaps[T <: AnyVal](a: Map[Int, Histogram[T]], b: Map[Int, Histogram[T]]) = {
    var res = a
    for ((k, v) <- b)
      res = res + (k ->
        (
          if (res.contains(k)) res(k).merge(v)
          else v
        )
      )

    res
  }

  def histogram[K: ClassTag](rdd: RDD[(K, Tile)], zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): Map[Int, Histogram[Int]] =
    partitioner
      .fold(rdd.join(zonesTileRdd))(rdd.join(zonesTileRdd, _))
      .map((t: (K, (Tile, Tile))) => IntZonalHistogram(t._2._1, t._2._2))
      .fold(Map[Int, Histogram[Int]]())(mergeMaps)

  def histogramDouble[K: ClassTag](rdd: RDD[(K, Tile)], zonestileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): Map[Int, Histogram[Double]] =
    partitioner
      .fold(rdd.join(zonestileRdd))(rdd.join(zonestileRdd, _))
      .map((t: (K, (Tile, Tile))) => DoubleZonalHistogram(t._2._1, t._2._2))
      .fold(Map[Int, Histogram[Double]]())(mergeMaps)

  def percentage[K: ClassTag](rdd: RDD[(K, Tile)], zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): RDD[(K, Tile)] = {
    val sc = rdd.sparkContext
    val zoneHistogramMap = histogram(rdd, zonesTileRdd, partitioner)
    val zoneSumMap = zoneHistogramMap.map { case (k, v) => k -> v.totalCount }
    val bcZoneHistogramMap = sc.broadcast(zoneHistogramMap)
    val bcZoneSumMap = sc.broadcast(zoneSumMap)

    rdd.combineValues(zonesTileRdd, partitioner) { case (tile, zone) =>
      val zhm = bcZoneHistogramMap.value
      val zsm = bcZoneSumMap.value

      val (cols, rows) = (tile.cols, tile.rows)

      val res = IntArrayTile.empty(cols, rows)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val (v, z) = (tile.get(col, row), zone.get(col, row))

          val (count, zoneCount) = (zhm(z).itemCount(v), zsm(z))

          res.set(col, row, math.round((count / zoneCount.toDouble) * 100).toInt)
        }
      }

      res: Tile
    }
  }
}
