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

package geotrellis.raster

import geotrellis.macros._

/**
  *  The [[MappableTile]] trait.
  */
trait MappableTile[T <: MappableTile[T]] extends MacroMappableTile[T] {

  /**
    * Map over the tiles using a function which accepts the column,
    * row, and value at that position and returns an integer.
    */
  def map(f: (Int, Int, Int) => Int): T =
    macro TileMacros.intMap_impl[T]

  /**
    * Map over the tiles using a function which accepts the column,
    * row, and value at that position and returns a double.
    */
  def mapDouble(f: (Int, Int, Double) => Double): T =
    macro TileMacros.doubleMap_impl[T]
}
