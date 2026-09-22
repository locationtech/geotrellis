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

import geotrellis.macros.*

import scala.quoted.*

/**
  *  The [[MappableTile]] trait.
  */
trait MappableTile[T <: MappableTile[T]] extends MacroMappableTile[T] {

  /**
    * Map over the tiles using a function which accepts the column,
    * row, and value at that position and returns an integer.
    */
  inline def map(inline f: (Int, Int, Int) => Int): T =
    ${ TileMacros.intMap_impl[T]('{ this.asInstanceOf[MacroMappableTile[T]] }, 'f) }

  /**
    * Map over the tiles using a function which accepts the column,
    * row, and value at that position and returns a double.
    */
  inline def mapDouble(inline f: (Int, Int, Double) => Double): T =
    ${ TileMacros.doubleMap_impl[T]('{ this.asInstanceOf[MacroMappableTile[T]] }, 'f) }
}
