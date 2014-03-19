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

import geotrellis.Raster
import geotrellis.RasterExtent

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.formats._
import geotrellis.spark.cmd.NoDataHandler
import geotrellis.spark.metadata.PyramidMetadata

case class Tile(id: Long, raster: Raster) {
  def tileXY(zoom: Int) =  TmsTiling.tileXY(id, zoom)

  def toWritable() = 
    (TileIdWritable(id), ArgWritable.fromRaster(raster))
}

object Tile {
  def toWritable(tr: Tile): WritableTile =
    (TileIdWritable(tr.id), ArgWritable.fromRasterData(tr.raster.data))
}
