/*
 * Copyright 2016-2017 Azavea
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

package geotrellis.raster.crop

import geotrellis.raster._
import geotrellis.vector._

object Implicits extends Implicits

/**
  * Trait housing the implicit class which add extension methods for
  * cropping to [[CellGrid]].
  */
trait Implicits {
  implicit class withExtentCropMethods[T <: CellGrid: (? => CropMethods[T])](self: Raster[T])
      extends RasterCropMethods[T](self)

  implicit class withTileFeatureCropMethods[
    T <: CellGrid: (? => TileCropMethods[T]), D
  ](self: TileFeature[T, D]) extends TileFeatureCropMethods[T, D](self)

  implicit class withRasterTileFeatureCropMethods[
    T <: CellGrid : (? => TileCropMethods[T]), D
  ](self: TileFeature[Raster[T], D]) extends RasterTileFeatureCropMethods[T, D](self)
}
