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

trait LocalTileBinaryOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if(n.endsWith("$")) n.substring(0, n.length - 1)
    else n
  }

  // Tile - Constant combinations

  /** Apply to the value from each cell and a constant Int. */
  def apply(r: Tile, c: Int): Tile =
    r.dualMap(combine(_, c))({
      val d = i2d(c)
      (z: Double) => combine(z, d)
    })

  /** Apply to the value from each cell and a constant Double. */
  def apply(r: Tile, c: Double): Tile =
    r.dualMap({z => d2i(combine(i2d(z), c))})(combine(_, c))

  /** Apply to a constant Int and the value from each cell. */
  def apply(c: Int, r: Tile): Tile =
    r.dualMap(combine(c, _))({
      val d = i2d(c)
      (z: Double) => combine(d, z)
    })

  /** Apply to a constant Double and the value from each cell. */
  def apply(c: Double, r: Tile): Tile =
    r.dualMap({z => d2i(combine(c, i2d(z)))})(combine(c, _))

  // Tile - Tile combinations

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1: Tile, r2: Tile): Tile = 
    r1.dualCombine(r2)(combine)(combine)

  // Combine a sequence of rasters

  /** Apply this operation to a Seq of rasters */
  def apply(rs: Traversable[Tile]): Tile = 
    new TileReducer(combine)(combine)(rs)

  def combine(z1: Int, z2: Int): Int
  def combine(z1: Double, z2: Double): Double
}
