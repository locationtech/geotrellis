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
import geotrellis.raster.histogram._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.rdd._

import scala.reflect.ClassTag

abstract class ZonalTileRDDMethods[K: ClassTag] extends MethodExtensions[RDD[(K, Tile)]] {
  def zonalHistogram(zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): Map[Int, Histogram[Int]] =
    Zonal.histogram(self, zonesTileRdd, partitioner)

  def zonalHistogramDouble(zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): Map[Int, Histogram[Double]] =
    Zonal.histogramDouble(self, zonesTileRdd, partitioner)

  def zonalPercentage(zonesTileRdd: RDD[(K, Tile)], partitioner: Option[Partitioner] = None): RDD[(K, Tile)] =
    Zonal.percentage(self, zonesTileRdd, partitioner)
}
