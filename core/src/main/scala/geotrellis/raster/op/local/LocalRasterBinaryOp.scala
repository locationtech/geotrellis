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

trait LocalRasterBinaryOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if(n.endsWith("$")) n.substring(0,n.length-1)
    else n
  }

  // Raster - Constant combinations

  /** Apply to the value from each cell and a constant Int. */
  def apply(r:Op[Raster], c:Op[Int]):Op[Raster] = 
    (r,c).map(apply)
         .withName(s"$name[ConstantInt]")

  /** Apply to the value from each cell and a constant Double. */
  def apply(r:Op[Raster], c:Op[Double])(implicit d:DI):Op[Raster] = 
    (r,c).map(apply)
         .withName(s"$name[ConstantDouble]")

  /** Apply to a constant Int and the value from each cell. */
  def apply(c:Op[Int],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI):Op[Raster] = 
    (c,r).map(apply)
         .withName(s"$name[ConstantInt]")

  /** Apply to a constant Double and the value from each cell. */
  def apply(c:Op[Double],r:Op[Raster])(implicit d:DI,d2:DI,d3:DI,d4:DI):Op[Raster] = 
    (c,r).map(apply)
         .withName(s"$name[ConstantDouble]")

  /** Apply to the value from each cell and a constant Int. */
  def apply(r: Raster, c: Int): Raster =
    r.dualMap(combine(_,c))({
      val d = i2d(c)
      (z:Double) => combine(z,d)
    })

  /** Apply to the value from each cell and a constant Double. */
  def apply(r: Raster, c: Double): Raster =
    r.dualMap({z => d2i(combine(i2d(z),c))})(combine(_,c))

  /** Apply to a constant Int and the value from each cell. */
  def apply(c: Int, r: Raster): Raster =
    r.dualMap(combine(c,_))({
      val d = i2d(c)
      (z:Double) => combine(d,z)
    })

  /** Apply to a constant Double and the value from each cell. */
  def apply(c: Double, r: Raster): Raster =
    r.dualMap({z => d2i(combine(c,i2d(z)))})(combine(c,_))

  // Raster - Raster combinations

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1:Op[Raster],r2:Op[Raster])(implicit d:DI,d2:DI):Op[Raster] = 
    (r1,r2).map(apply)
           .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1: Raster,r2: Raster): Raster = 
    r1.dualCombine(r2)(combine)(combine)

  // Combine a sequence of rasters

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Seq[Op[Raster]]):Op[Raster] = 
    rs.mapOps(apply)
      .withName(s"$name[Rasters]")

  /** Apply this operation to a Seq of rasters */
  def apply(rs:Seq[Raster]):Raster = 
    new RasterReducer(combine)(combine)(rs)

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Array[Op[Raster]]):Op[Raster] = 
    apply(rs.toSeq)

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Seq[Raster]]):Op[Raster] = 
    rs.map(apply)
      .withName("$name[Rasters-Literal]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Array[Raster]])(implicit d:DI):Op[Raster] = 
    apply(rs.map(_.toSeq).withName("$name[Rasters]"))

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Seq[Op[Raster]]])(implicit d:DI,d2:DI):Op[Raster] =
    rs.flatMap { seq:Seq[Op[Raster]] => apply(seq:_*) }
      .withName(s"$name[Rasters]")

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(rs:Op[Raster]*)(implicit d:DI,d2:DI,d3:DI):Op[Raster] = {
    apply(rs)
  }

  def combine(z1:Int,z2:Int):Int
  def combine(z1:Double,z2:Double):Double
}
