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

package geotrellis.spark.render

import geotrellis.raster.Tile
import geotrellis.raster.render._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD

trait SpatialTileRDDRenderMethods extends MethodExtensions[RDD[(SpatialKey, Tile)]] {
  def color(colorMap: ColorMap): RDD[(SpatialKey, Tile)] =
    self.mapValues(_.color(colorMap))

  /**
    * Renders each tile as a PNG. Assumes tiles are already colors of RGBA values.
    */
  def renderPng(): RDD[(SpatialKey, Png)] =
    Render.renderPng(self)

  /**
    * Renders each tile as a PNG.
    *
    * @param colorMap    ColorMap to use when rendering tile values to color.
    */
  def renderPng(colorMap: ColorMap): RDD[(SpatialKey, Png)] =
    Render.renderPng(self, colorMap)

  /**
    * Renders each tile as a JPG. Assumes tiles are already colors of RGBA values.
    */
  def renderJpg(): RDD[(SpatialKey, Jpg)] =
    Render.renderJpg(self)

  /**
    * Renders each tile as a JPG.
    *
    * @param colorMap    ColorMap to use when rendering tile values to color.
    */
  def renderJpg(colorMap: ColorMap): RDD[(SpatialKey, Jpg)] =
    Render.renderJpg(self, colorMap)
}
