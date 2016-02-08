/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.render

import  geotrellis.raster.render.jpg._
import  geotrellis.raster.render.png._
import geotrellis._
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.histogram.Histogram

import scala.collection.mutable

case class Renderer(colorMap: ColorMap, cellType: CellType, colorType: PngColorEncoding) {
  def render(r: Tile) =
    colorMap.render(r).convert(cellType)
}

object Renderer {
  /** Include a precomputed histogram to cache the color map and speed up the rendering. */
  def apply(colorClassifier: ColorClassifier, h: Option[Histogram]): Renderer = {
    val colorMap = colorClassifier.toColorMap()
    val pngEncoding = PngColorEncoding.fromRasterColorClassifier(colorClassifier)
    h match {
      case Some(hist) =>
        Renderer(colorMap.cache(hist), TypeByte, pngEncoding)
      case None =>
        Renderer(colorMap, TypeByte, pngEncoding)
    }
  }
}
