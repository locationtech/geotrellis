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
import geotrellis.util.MethodExtensions
import cats.Semigroup


abstract class RasterTileFeatureMergeMethods[
  T <: CellGrid[Int] : * => TileMergeMethods[T],
  D : Semigroup
](val self: TileFeature[Raster[T], D])(implicit ev0: Raster[T] => RasterMergeMethods[T]) extends MethodExtensions[TileFeature[Raster[T], D]] {

  def merge(other: TileFeature[Raster[T], D]): TileFeature[Raster[T], D] =
    TileFeature(self.tile.merge(other.tile), Semigroup[D].combine(self.data, other.data))

  def merge(other: TileFeature[Raster[T], D], method: ResampleMethod): TileFeature[Raster[T], D] =
    TileFeature(self.tile.merge(other.tile, method), Semigroup[D].combine(self.data, other.data))

  def union(other: TileFeature[Raster[T], D], method: ResampleMethod, unionFunc: (Option[Double], Option[Double]) => Double): TileFeature[Raster[T], D] =
    TileFeature(self.tile.union(other.tile, method, unionFunc), Semigroup[D].combine(self.data, other.data))
}
