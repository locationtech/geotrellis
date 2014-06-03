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

trait LocalTileBinaryOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if(n.endsWith("$")) n.substring(0, n.length - 1)
    else n
  }

  // Tile - Constant combinations

  /** Apply to the value from each cell and a constant Int. */
  def apply(r: Op[Tile], c: Op[Int]): Op[Tile] = 
    (r, c).map(apply)
         .withName(s"$name[ConstantInt]")

  /** Apply to the value from each cell and a constant Double. */
  def apply(r: Op[Tile], c: Op[Double])(implicit d: DI): Op[Tile] = 
    (r, c).map(apply)
         .withName(s"$name[ConstantDouble]")

  /** Apply to a constant Int and the value from each cell. */
  def apply(c: Op[Int], r: Op[Tile])(implicit d: DI, d2: DI, d3: DI): Op[Tile] = 
    (c, r).map(apply)
         .withName(s"$name[ConstantInt]")

  /** Apply to a constant Double and the value from each cell. */
  def apply(c: Op[Double], r: Op[Tile])(implicit d: DI, d2: DI, d3: DI, d4: DI): Op[Tile] = 
    (c, r).map(apply)
         .withName(s"$name[ConstantDouble]")

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
  def apply(r1: Op[Tile], r2: Op[Tile])(implicit d: DI, d2: DI): Op[Tile] = 
    (r1, r2).map(apply)
           .withName(s"$name[Tiles]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1: Tile, r2: Tile): Tile = 
    r1.dualCombine(r2)(combine)(combine)

  // Combine a sequence of rasters

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs: Seq[Op[Tile]]): Op[Tile] = 
    rs.mapOps(apply)
      .withName(s"$name[Tiles]")

  /** Apply this operation to a Seq of rasters */
  def apply(rs: Seq[Tile]): Tile = 
    new TileReducer(combine)(combine)(rs)

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs: Array[Op[Tile]]): Op[Tile] = 
    apply(rs.toSeq)

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs: Op[Seq[Tile]]): Op[Tile] = 
    rs.map(apply)
      .withName("$name[Tiles - Literal]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs: Op[Array[Tile]])(implicit d: DI): Op[Tile] = 
    apply(rs.map(_.toSeq).withName("$name[Tiles]"))

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs: Op[Seq[Op[Tile]]])(implicit d: DI, d2: DI): Op[Tile] =
    rs.flatMap { seq: Seq[Op[Tile]] => apply(seq: _*) }
      .withName(s"$name[Tiles]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs: Op[Tile]*)(implicit d: DI, d2: DI, d3: DI): Op[Tile] = {
    apply(rs)
  }

  def combine(z1: Int, z2: Int): Int
  def combine(z1: Double, z2: Double): Double
}
