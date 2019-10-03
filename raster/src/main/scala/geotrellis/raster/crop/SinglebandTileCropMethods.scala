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
import geotrellis.raster.io.geotiff._

/**
  * A trait housing extension methods for cropping [[Tile]]s.
  */
trait SinglebandTileCropMethods extends TileCropMethods[Tile] {
  import Crop.Options

  /**
    * Given a [[GridBounds]] and some cropping options, produce a new
    * [[Tile]].
    */
  def crop(gridBounds: GridBounds[Int], options: Options): Tile = {
    if (!gridBounds.intersects(self.dimensions)) throw GeoAttrsError(s"$gridBounds do not intersect ${self.dimensions}")
    val cropBounds =
      if(options.clamp) gridBounds.intersection(self.dimensions).get
      else gridBounds

    val res =
      self match {
        case gtTile: GeoTiffTile => gtTile.crop(gridBounds)
        case _ => CroppedTile(self, cropBounds)
      }

    if(options.force) res.toArrayTile else res
  }

  /**
    * Given a source Extent, a destination Extent, and some cropping
    * options, produce a cropped [[Raster]].
    */
  def crop(srcExtent: Extent, extent: Extent, options: Options): Tile =
    Raster(self, srcExtent).crop(extent, options).tile
}
