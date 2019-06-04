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

package geotrellis.spark.store.slippy

import geotrellis.tiling.SpatialKey
import geotrellis.spark._

import org.apache.spark._
import org.apache.spark.rdd._

trait SlippyTileReader[T] {
  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)]
  def read(zoom: Int, key: SpatialKey): T
  def read(zoom: Int, x: Int, y: Int): T =
    read(zoom, SpatialKey(x, y))
}

object SlippyTileReader {
  val TilePath = """.*/(\d+)/(\d+)\.\w+$""".r
}
