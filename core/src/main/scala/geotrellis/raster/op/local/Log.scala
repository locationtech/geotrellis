/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.local

import geotrellis._

/**
 * Computes the Log of Raster values.
 */
object Log extends Serializable {
  /** Computes the Log of a Raster. */
  def apply(r: Raster): Raster =
    r.dualMap { z: Int => if(isNoData(z)) z else math.log(z).toInt }
              { z: Double => math.log(z) }
}

/**
 * Operation to get the Log base 10 of values.
 */
object Log10 extends Serializable {
  /** Takes the Log base 10 of each raster cell value. */
  def apply(r: Raster): Raster =
    r.dualMap { z: Int => if(isNoData(z)) z else math.log10(z).toInt }
              { z: Double => math.log10(z) }
}
