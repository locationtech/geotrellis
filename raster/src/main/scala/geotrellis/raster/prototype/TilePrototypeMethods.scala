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

package geotrellis.raster.prototype

import geotrellis.raster._
import geotrellis.util.MethodExtensions


/**
  * Methods that allow us to create a similar tile type from an
  * already existing tile.
  */
trait TilePrototypeMethods[T <: CellGrid] extends MethodExtensions[T] {

  /**
    * Given numbers of columns and rows, produce a new [[ArrayTile]]
    * of the given size and the same band count as the calling object.
    */
  def prototype(cols: Int, rows: Int): T

  /**
    * Given a [[CellType]] and numbers of columns and rows, produce a
    * new [[ArrayTile]] of the given size and the same band count as
    * the calling object.
    */
  def prototype(cellType: CellType, cols: Int, rows: Int): T
}
