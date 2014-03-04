/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.raster.op.local

import geotrellis._

object InverseMask extends Serializable {
/**
 * Generate a raster with the values from the first raster, but only include
 * cells in which the corresponding cell in the second raster is set to the 
 * "readMask" value. 
 *
 * For example, if *all* cells in the second raster are set to the readMask value,
 * the output raster will be identical to the first raster.
 */
  def apply(r1:Op[Raster], r2:Op[Raster], readMask:Op[Int], writeMask:Op[Int]) =
    (r1,r2,readMask,writeMask).map { (r1,r2,readMask,writeMask) =>
      r1.dualCombine(r2)((z1,z2) => 
        if (z2 == readMask) z1 else writeMask
      )((z1,z2) => 
        if (d2i(z2) == readMask) z1 else i2d(writeMask)
      )
    }.withName("InverseMask")
}
