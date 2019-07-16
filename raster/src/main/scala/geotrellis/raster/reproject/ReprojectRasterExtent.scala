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
   */
  def apply[N: Integral](ge: GridExtent[N], transform: Transform, resampleGrid: ResampleGrid[N]): GridExtent[N] = {
    val extent = ge.extent
    val newExtent = extent.reprojectAsPolygon(transform, 0.001).extent

    resampleGrid match {
      case TargetGrid(grid) =>
        grid.createAlignedGridExtent(newExtent).toGridType[N]
      case TargetCellSize(cs) =>
        reprojectedGridForCellSize(cs, newExtent)
      case TargetDimensions(cols, rows) =>
        ???  // Nope, not a sensible option
      case TargetRegion(region) =>
        region  // This feels kind of strange
      case IdentityResampleGrid =>
        // Obviously i'm not preserving the identity of the underlying gridextent
        // not sure how the default case should be modeled with this resample target AST
        val cs = {
          val distance = newExtent.northWest.distance(newExtent.southEast)
          val cols = ge.extent.width / ge.cellwidth
          val rows = ge.extent.height / ge.cellheight
          val pixelSize = distance / math.sqrt(cols * cols + rows * rows)
          CellSize(pixelSize, pixelSize)
        }
        reprojectedGridForCellSize(cs, newExtent)
    }
  }

  def apply[N: Integral](ge: GridExtent[N], src: CRS, dest: CRS, resampleGrid: ResampleGrid[N]): GridExtent[N] =
    apply(ge, Transform(src, dest), resampleGrid)

  def apply[N: Integral](re: RasterExtent, transform: Transform, resampleGrid: ResampleGrid[N]): RasterExtent =
    apply(re.toGridType[N]: GridExtent[N], transform, resampleGrid).toRasterExtent

  def apply[N: Integral](re: RasterExtent, src: CRS, dest: CRS, resampleGrid: ResampleGrid[N]): RasterExtent =
    apply(re, Transform(src, dest), resampleGrid)

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
