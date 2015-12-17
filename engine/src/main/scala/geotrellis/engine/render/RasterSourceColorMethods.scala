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

package geotrellis.engine.render

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.engine._

trait RasterSourceColorMethods extends RasterSourceMethods {
  def color(breaksToColors: Map[Int, Int]): RasterSource =
    color(breaksToColors, ColorMapOptions.Default)

  def color(breaksToColors: Map[Int, Int], options: ColorMapOptions): RasterSource =
    rasterSource.mapTile(_.color(breaksToColors, options))

  def color(breaksToColors: Map[Double, Int])(implicit d: DI): RasterSource =
    color(breaksToColors, ColorMapOptions.Default)

  def color(breaksToColors: Map[Double, Int], options: ColorMapOptions)(implicit d: DI): RasterSource =
    rasterSource.mapTile(_.color(breaksToColors, options))
}
