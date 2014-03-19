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

package geotrellis.raster.op.transform

import geotrellis._
import geotrellis.data._
/**
 * Generate a raster with a new extent and resolution.
 *
 * This uses a nearest-neighbor algorithm to resize a raster.
 *
 * @note               Resample does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Resize(r:Op[Raster], rasterExtent:Op[RasterExtent]) 
	extends Op2(r,rasterExtent) ({
  (raster,rasterExtent) =>
    Result(raster.warp(rasterExtent))
})

object Resize {
  /**
   * Generate a raster with a new extent and resolution. 
   */
  def apply(r:Op[Raster], extentOp:Op[Extent], cols:Op[Int], rows:Op[Int]):Op[Raster] = 
    (extentOp,cols,rows).flatMap( (e,cs,rs) => Resize(r,RasterExtent(e, cs, rs)))

  def apply(r:Op[Raster], cols:Op[Int], rows:Op[Int]):ResizeGrid = ResizeGrid(r, cols, rows)
}

case class ResizeGrid(r:Op[Raster], cols:Op[Int], rows:Op[Int])
extends Op3(r,cols,rows)({
  (raster,cols,rows) => {
    val extent = raster.rasterExtent.extent
    val cw = extent.width / cols
    val ch = extent.height / rows
    val re = RasterExtent(extent, cw, ch, cols, rows)
    Result(raster.warp(re))
  }
})

