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

import geotrellis._
import geotrellis.raster._
import geotrellis.process._

/**
 * Maps values to 0 if the are NoData values, otherwise 1.
 */
object Defined extends Serializable {
  /** Maps an integer typed Tile to 1 if the cell value is not NODATA, otherwise 0. */
  def apply(r: Tile): Tile = 
    r.map { z: Int => if(isNoData(z)) 0 else 1 }
     .convert(TypeBit)
}
