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

package geotrellis.raster.prototype

import geotrellis.raster._
import cats.Monoid

class TileFeaturePrototypeMethods[
  T <: CellGrid : (? => TilePrototypeMethods[T]),
  D: Monoid
](val self: TileFeature[T, D]) extends TilePrototypeMethods[TileFeature[T, D]] {
  def prototype(cols: Int, rows: Int): TileFeature[T, D] =
    TileFeature(self.tile.prototype(cols, rows), Monoid[D].empty)

  def prototype(cellType: CellType, cols: Int, rows: Int): TileFeature[T, D] =
    TileFeature(self.tile.prototype(cellType, cols, rows), Monoid[D].empty)
}
