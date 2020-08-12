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

package geotrellis.raster.transform

import geotrellis.raster.{CellGrid, MultibandTile, Raster, Tile}


abstract class RasterTransformMethods[T <: CellGrid[Int]: * => TransformMethods[T]] extends TransformMethods[Raster[T]] {
  def rotate90(n: Int = 1): Raster[T] = Raster(self.tile.rotate90(n), self.extent)
  def flipVertical: Raster[T] = Raster(self.tile.flipVertical, self.extent)
  def flipHorizontal: Raster[T] = Raster(self.tile.flipHorizontal, self.extent)
}

trait SinglebandRasterTransformMethods extends RasterTransformMethods[Tile]
trait MultibandRasterTransformMethods extends RasterTransformMethods[MultibandTile]
