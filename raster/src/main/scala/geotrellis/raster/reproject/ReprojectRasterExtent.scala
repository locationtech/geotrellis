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
import geotrellis.vector._
import geotrellis.proj4._


import spire.math.Integral


object ReprojectRasterExtent {
  import Reproject.Options

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
  def apply[N: Integral](ge: GridExtent[N], transform: Transform, options: Options): GridExtent[N] = {
    val extent = ge.extent
    val newExtent = extent.reprojectAsPolygon(transform, 0.001).extent

    options.parentGridExtent match {
      case Some(parentGridExtent) =>
        parentGridExtent.createAlignedGridExtent(newExtent).toGridType[N]

      case None =>
        val (pixelSizeX, pixelSizeY) =
          options.targetCellSize match {
            case Some(cellSize) =>
              (cellSize.width, cellSize.height)
            case None =>
              val distance = newExtent.northWest.distance(newExtent.southEast)
              val cols = ge.extent.width / ge.cellwidth
              val rows = ge.extent.height / ge.cellheight
              val pixelSize = distance / math.sqrt(cols * cols + rows * rows)
              (pixelSize, pixelSize)
          }

        val newCols = (newExtent.width / pixelSizeX + 0.5).toLong
        val newRows = (newExtent.height / pixelSizeY + 0.5).toLong

        //Adjust the extent to match the pixel size.
        val adjustedExtent = Extent(newExtent.xmin, newExtent.ymax - (pixelSizeY*newRows), newExtent.xmin + (pixelSizeX*newCols), newExtent.ymax)

        // TODO: consider adding .withExtent and .withCellSize to GridExtent so we can parametrize this function on N, T <: GridExtent[N] and keep the type T
        // ^ this would also remove the requirement of : Integral on N
        new GridExtent[N](adjustedExtent, CellSize(pixelSizeX, pixelSizeY))
    }
  }

  def apply[N: Integral](ge: GridExtent[N], transform: Transform): GridExtent[N] =
    apply(ge, transform, Options.DEFAULT)

  def apply[N: Integral](ge: GridExtent[N], src: CRS, dest: CRS, options: Options): GridExtent[N] =
    if(src == dest) {
      ge
    } else {
      apply(ge, Transform(src, dest), options)
    }

  def apply[N: Integral](ge: GridExtent[N], src: CRS, dest: CRS): GridExtent[N] =
    apply(ge, src, dest, Options.DEFAULT)

  def apply(re: RasterExtent, transform: Transform, options: Reproject.Options): RasterExtent =
    apply(re: GridExtent[Int], transform, options).toRasterExtent

  def apply(re: RasterExtent, transform: Transform): RasterExtent =
    apply(re, transform, Options.DEFAULT)

  def apply(re: RasterExtent, src: CRS, dest: CRS, options: Options): RasterExtent =
    if(src == dest) {
      re
    } else {
      apply(re, Transform(src, dest), options)
    }

  def apply(re: RasterExtent, src: CRS, dest: CRS): RasterExtent =
    apply(re, src, dest, Options.DEFAULT)

}
