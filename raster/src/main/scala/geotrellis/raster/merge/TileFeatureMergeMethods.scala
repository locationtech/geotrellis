/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector._
import cats.Semigroup


abstract class TileFeatureMergeMethods[
  T <: CellGrid[Int]: * => TileMergeMethods[T],
  D: Semigroup
](val self: TileFeature[T, D]) extends TileMergeMethods[TileFeature[T, D]] {
  def merge(other: TileFeature[T, D], baseCol: Int, baseRow: Int): TileFeature[T, D] =
    TileFeature(self.tile.merge(other.tile, baseCol, baseRow), Semigroup[D].combine(self.data, other.data))

  def merge(extent: Extent, otherExtent: Extent, other: TileFeature[T, D], method: ResampleMethod): TileFeature[T, D] =
    TileFeature(self.tile.merge(extent, otherExtent, other.tile, method), Semigroup[D].combine(self.data, other.data))
}
