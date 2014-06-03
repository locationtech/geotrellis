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

/**
 * Rescale the size of a raster by a fixed percentage, maintaining the aspect ratio.
 * 
 * This operation uses a nearest neighbor algorithm to resize a raster by a 
 * given percentage.
 * 
 * For example, if the rescale percentage is 2.0, the number of columns and
 * the number of rows will be doubled.  If the original raster had 100 columns 
 * and 100 rows and each cell had a cell width and a cell height in map units 
 * of 50m, the new raster would be 200x200 (cols and rows) and the new cell width 
 * and height would be 25m.
 * 
 * @param rescalePct  Fraction of input raster size.
 *
 * @note               Rescale does not currently support Double raster data.
 *                     If you use a Raster with a Double CellType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
// case class Rescale(r:Op[Raster], rescalePct:Op[Double]) extends Op2(r,rescalePct) ({
//   (r,rescalePct) => {
//     val re = r.rasterExtent
//     val cw = re.cellwidth / rescalePct
//     val ch = re.cellheight / rescalePct
//     val newRasterExtent = re.withResolution(cw, ch)
//     Result(r.warp(newRasterExtent))
//   }
// })

// TODO: DELETE?
