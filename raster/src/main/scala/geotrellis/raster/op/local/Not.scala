/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.local

import geotrellis.raster._

/**
 * Bitwise negation of Tile.
 * @note               NotRaster does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Not extends Serializable {
  /** Returns the bitwise negation of each cell value. */
  def apply(r: Tile): Tile = 
    r.map { z => if(isNoData(z)) z else ~z }
}
