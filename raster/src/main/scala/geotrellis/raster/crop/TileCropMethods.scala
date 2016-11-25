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

package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._


/**
  * A trait guaranteeing extension methods for cropping [[Tile]]s.
  */
trait TileCropMethods[T <: CellGrid] extends CropMethods[T] {
  import Crop.Options

  /**
    * Given a source Extent, a destination extent, and some cropping
    * options, produce a cropped [[Tile]].
    */
  def crop(srcExtent: Extent, extent: Extent, options: Options): T

  /**
    * Given a source Extent and a destination extent produce a cropped
    * [[Tile]].
    */
  def crop(srcExtent: Extent, extent: Extent): T =
    crop(srcExtent, extent, Options.DEFAULT)

}
