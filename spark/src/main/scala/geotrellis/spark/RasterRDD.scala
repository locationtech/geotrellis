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
import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

class RasterRDD[K: ClassTag](tileRdd: RDD[(K, Tile)], val metaData: RasterMetaData) extends GeoRDD[K, Tile](tileRdd) {
  type Self = RasterRDD[K]

  def wrap(f: => RDD[(K, Tile)]): Self =
    new RasterRDD[K](f, metaData)

  def convert(cellType: CellType): RasterRDD[K] =
    mapTiles(_.convert(cellType))

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
}
