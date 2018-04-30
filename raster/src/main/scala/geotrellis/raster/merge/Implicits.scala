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

package geotrellis.raster.merge

import geotrellis.raster._
import cats.Semigroup

object Implicits extends Implicits

/**
  * A trait holding the implicit class which makes the extensions
  * methods available.
  */
trait Implicits {
  implicit class withRasterMergeMethods[T <: CellGrid: ? => TileMergeMethods[T]](self: Raster[T]) extends RasterMergeMethods[T](self)

  implicit class withTileFeatureMergeMethods[
    T <: CellGrid: ? => TileMergeMethods[T],
    D: Semigroup
  ](self: TileFeature[T, D]) extends TileFeatureMergeMethods[T,D](self)

  implicit class withRasterTileFeatureMergeMethods[
    T <: CellGrid: ? => TileMergeMethods[T],
    D: Semigroup
  ](self: TileFeature[Raster[T], D]) extends RasterTileFeatureMergeMethods[T,D](self)
}
