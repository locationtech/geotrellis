/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.raster.rasterize

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterize.Options
import geotrellis.vector.{Geometry,Feature}
import geotrellis.util.MethodExtensions


trait FeatureIntRasterizeMethods[+G <: Geometry, T <: Feature[G,Int]] extends MethodExtensions[T] {
  def foreachCell(re : RasterExtent, options: Options = Options.DEFAULT) : Tile = {
    val geom = self.geom
    val data = self.data
    geom.foreachCell(re, options)({ (x,y) => data })
  }

  def foreachCellDouble(re : RasterExtent, options: Options = Options.DEFAULT) : Tile = {
    val geom = self.geom
    val data = self.data
    geom.foreachCellDouble(re, options)({ (x,y) => data.toDouble })
  }
}

trait FeatureDoubleRasterizeMethods[+G <: Geometry, T <: Feature[G,Double]] extends MethodExtensions[T] {
  def foreachCell(re : RasterExtent, options: Options = Options.DEFAULT) : Tile = {
    val geom = self.geom
    val data = self.data
    geom.foreachCell(re, options)({ (x,y) => data.toInt })
  }

  def foreachCellDouble(re : RasterExtent, options: Options = Options.DEFAULT) : Tile = {
    val geom = self.geom
    val data = self.data
    geom.foreachCellDouble(re, options)({ (x,y) => data })
  }
}
