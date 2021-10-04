/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster.{BufferTile, Tile}
import shapeless.=:!=

object Implicits extends Implicits

trait Implicits {
  /**
   * TODO: think of a better way of organizing implicits with regards to possible Tile successors
   *
   * Apply withTileFocalMethods to all successor of a [[Tile]] type but [[BufferTile]].
   * It is required to disambiguate withBufferTileFocalMethods implicit.
   */
  implicit class withTileFocalMethods[T](tile: T)(implicit t: T <:< Tile, nbt: T =:!= BufferTile) extends FocalMethods {
    val self: Tile = t(tile)
  }

  /**
   * [[BufferTile]] can't have successors (it is a case class).
   * Limit this implicit to be applied to the [[BufferTile]] type only.
   */
  implicit class withBufferTileFocalMethods[T](tile: T)(implicit bt: T =:= BufferTile) extends BufferTileFocalMethods {
    val self: BufferTile = bt(tile)
  }
}
