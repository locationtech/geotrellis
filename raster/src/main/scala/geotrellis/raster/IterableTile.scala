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
  * Trait to supply foreach(|Double) methods.
  */
trait IterableTile extends MacroIterableTile {

  /**
    * Execute the given function 'f' at every location in the tile.
    * The function 'f' takes the column, row, and value and returns
    * nothing (presumably for side-effects).
    */
  def foreach(f: (Int, Int, Int) => Unit): Unit =
    macro TileMacros.intForeach_impl

  /**
    * Execute the given function 'f' at every location in the tile.
    * The function 'f' takes the column, row, and value, the last one
    * as a double, and returns nothing (presumably for side-effects).
    */
  def foreachDouble(f: (Int, Int, Double) => Unit): Unit =
    macro TileMacros.doubleForeach_impl
}
