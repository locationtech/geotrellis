/*
 * Copyright 2017 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import geotrellis.proj4.CRS
import geotrellis.vector._

class ProjectedRasterExtent(
  val projectedExtent: ProjectedExtent, 
  override val cellwidth: Double,
  override val cellheight: Double,
  cols: Int,
  rows: Int
) extends RasterExtent(projectedExtent.extent, cellwidth, cellheight, cols, rows) {
  val crs = projectedExtent.crs
}

object ProjectedRasterExtent {

  def apply(projectedExtent: ProjectedExtent, cellwidth: Double, cellheight: Double, cols: Int, rows: Int) =
    new ProjectedRasterExtent(projectedExtent, cellwidth, cellheight, cols, rows)

  def apply(extent: Extent, crs: CRS, cols: Int, rows: Int) = 
    new ProjectedRasterExtent(ProjectedExtent(extent, crs), 
                              extent.width / cols, 
                              extent.height / rows,
                              cols,
                              rows)

  def apply(projectedExtent: ProjectedExtent, cols: Int, rows: Int) = 
    new ProjectedRasterExtent(projectedExtent, 
                              projectedExtent.extent.width / cols, 
                              projectedExtent.extent.height / rows,
                              cols,
                              rows)

  def apply(re: RasterExtent, crs: CRS): ProjectedRasterExtent = 
    apply(re.extent, crs, re.cols, re.rows)

  def unapply(projRE: ProjectedRasterExtent): Option[(ProjectedExtent, Double, Double, Int, Int)] =
    Some(projRE.projectedExtent, projRE.cellwidth, projRE.cellheight, projRE.cols, projRE.rows)

}
