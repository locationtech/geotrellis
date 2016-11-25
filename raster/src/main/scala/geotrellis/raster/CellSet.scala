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


/**
  * A lighweight wrapper around performing foreach calculations on a
  * set of cell coordinates
  */
trait CellSet {
  /**
    * Calls a funtion with the col and row coordinate of every cell
    * contained in the CellSet.
    *
    * @param  f  Function that takes col and row coordinates, that will be called for each cell contained in this CellSet.
    */
  def foreach(f: (Int, Int)=>Unit): Unit
}
