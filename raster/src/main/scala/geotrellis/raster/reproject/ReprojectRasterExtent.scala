/*
 * Copyright 2016 Azavea
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

package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector._
import geotrellis.proj4._

import org.locationtech.jts.densify.Densifier

import spire.syntax.cfor._
import spire.math.Integral


object ReprojectRasterExtent {

  /** Reproject a [[RasterExtent]]
   *
   * @param ge The region to be reprojected
   * @param transform The reprojection to carry out
   * @param resampleTarget Optional resample target. If provided, attempt to
   * transform the reprojected extent into a [[GridExtent]] which satisfies the supplied
   * constraint. Otherwise, a heuristic ported from GDAL is used to approximate the
   * resolution of the input data.
   * @see [[reprojectedGridForCellSize]] defined below
   */
  def apply[N: Integral](ge: GridExtent[N], transform: Transform, resampleTarget: Option[ResampleTarget]): GridExtent[N] = {
    val extent = ge.extent
    val newExtent = extent.reprojectAsPolygon(transform, 0.001).extent

    resampleTarget match {
      case Some(TargetCellSize(cs)) =>
        reprojectedGridForCellSize(cs, newExtent)
      case Some(target: ResampleTarget) =>
        val cols = Integral[N].fromDouble(newExtent.width / ge.cellSize.width + 0.5)
        val rows = Integral[N].fromDouble(newExtent.height / ge.cellSize.height + 0.5)
        target(new GridExtent(newExtent, cols, rows))
      case None =>
        val cs = cellSizeHeuristic(ge, newExtent)
        reprojectedGridForCellSize(cs, newExtent)
    }
  }

  def apply[N: Integral](ge: GridExtent[N], src: CRS, dest: CRS, resampleTarget: Option[ResampleTarget]): GridExtent[N] =
    apply(ge, Transform(src, dest), resampleTarget)

  def apply(re: RasterExtent, transform: Transform, resampleTarget: Option[ResampleTarget]): RasterExtent =
    apply(re: GridExtent[Int], transform, resampleTarget).toRasterExtent

  def apply[N: Integral](re: RasterExtent, src: CRS, dest: CRS, resampleTarget: Option[ResampleTarget]): RasterExtent =
    apply(re, Transform(src, dest), resampleTarget)

  /** A resolution is computed with the intent that the length of the
   * distance from the top left corner of the output imagery to the bottom right
   * corner would represent the same number of pixels as in the source image.
   * Note that if the image is somewhat rotated the diagonal taken isnt of the
   * whole output bounding rectangle, but instead of the locations where the
   * top/left and bottom/right corners transform.  The output pixel size is
   * always square.  This is intended to approximately preserve the resolution
   * of the input data in the output file.
   *
   * This is a conceptual port of GDALSuggestedWarpOutput2, part of GDAL.
   * Docstring paraphrased from that code.
   * [[https://gdal.org/doxygen/gdal__alg_8h.html#a8ae26881b86e42ff958a8e81c4976fb3]]
   */
  private def cellSizeHeuristic[N: Integral](ge: GridExtent[N], newExtent: Extent): CellSize = {
    val distance = newExtent.northWest.distance(newExtent.southEast)
    val cols = ge.extent.width / ge.cellwidth
    val rows = ge.extent.height / ge.cellheight
    val pixelSize = distance / math.sqrt(cols * cols + rows * rows)
    CellSize(pixelSize, pixelSize)
  }

  /** Calculate the region and grid that best fits a provided cellsize and extent
   *
   * @note this behavior differs from [[GridExtent.withResolution]] in that it will reshape the output
   * extent to avoid cell remainders (i.e. extent width/height should be evenly divisible by cell width/height)
   */
  private def reprojectedGridForCellSize[N: Integral](cs: CellSize, newExtent: Extent): GridExtent[N] = {
    val newCols = (newExtent.width / cs.width + 0.5).toLong
    val newRows = (newExtent.height / cs.height + 0.5).toLong

    //Adjust the extent to match the pixel size.
    val adjustedExtent =
      Extent(newExtent.xmin, newExtent.ymax - (cs.height * newRows), newExtent.xmin + (cs.width * newCols), newExtent.ymax)

    // TODO: consider adding .withExtent and .withCellSize to GridExtent so we can parametrize this function on N, T <: GridExtent[N] and keep the type T
    // ^ this would also remove the requirement of : Integral on N
    new GridExtent[N](adjustedExtent, cs)
  }
}
