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

package geotrellis.raster.op.global

import geotrellis._
import geotrellis.raster._

/**
 * We'd like to use the normalize name both when we want to automatically
 * detect the min/max values, and when we provide them explicitly.
 *
 * @note               Normalize does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Normalize {
  @deprecated("Use Rescale.","0.8")
  def apply(r:Op[Tile], g:Op[(Int, Int)]) = AutomaticRescale(r, g)
  @deprecated("Use Rescale.","0.8")
  def apply(r:Op[Tile], c:Op[(Int, Int)], g:Op[(Int, Int)]) = PrecomputedRescale(r, c, g)
}

/**
 * Rescale a raster when we want to automatically detect the min/max values,
 * and when we provide them explicitly.
 *
 * @note               Rescale does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Rescale extends Serializable {
  /** Normalize the values in the given raster so that all values are within the
   * specified minimum and maximum value range. See [[AutomaticRescale]]. */
  def apply(r:Op[Tile], g:Op[(Int, Int)]) = AutomaticRescale(r, g)
  /** Normalize the values in the given raster.
   * zmin and zmax are the lowest and highest values in the provided raster.
   * gmin and gmax are the desired minimum and maximum values in the output raster.
   * See [[PrecomputedRescale]]. */
  def apply(r:Op[Tile], c:Op[(Int, Int)], g:Op[(Int, Int)]) = PrecomputedRescale(r, c, g)
}

/**
 * Normalize the values in the given raster so that all values are within the
 * specified minimum and maximum value range.
 * @note               AutomaticRescale does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class AutomaticRescale(r:Op[Tile], g:Op[(Int, Int)]) extends Op2(r,g) ({
  (raster,goal) => 
    val (zmin, zmax) = raster.findMinMax
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
})

/**
 * Normalize the values in the given raster.
 * zmin and zmax are the lowest and highest values in the provided raster.
 * gmin and gmax are the desired minimum and maximum values in the output raster.
 *
 * @note               PrecomputedRescale does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */ 
case class PrecomputedRescale(r:Op[Tile], c:Op[(Int, Int)],
                                g:Op[(Int, Int)]) extends Op3(r,c,g) ({
  (raster,curr,goal) =>
    val (zmin, zmax) = curr
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
})
