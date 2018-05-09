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

package geotrellis.raster.merge

import geotrellis.raster.resample.ResampleMethod
import geotrellis.raster.{ArrayMultibandTile, Tile, MultibandTile}
import geotrellis.vector.Extent

/**
  * A trait containing extension methods related to MultibandTile
  * merging.
  */
trait MultibandTileMergeMethods extends TileMergeMethods[MultibandTile] {

  def merge(other: MultibandTile, baseCol: Int, baseRow: Int): MultibandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until self.bandCount
      } yield {
        val thisBand = self.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(thatBand, baseCol, baseRow)
      }

    ArrayMultibandTile(bands)
  }

  /**
    * Merge this [[MultibandTile]] with the other one.  All places in
    * the present tile that contain NODATA and are in the intersection
    * of the two given extents are filled-in with data from the other
    * tile.  A new MutlibandTile is returned.
    *
    * @param   extent        The extent of this MultiBandTile
    * @param   otherExtent   The extent of the other MultiBandTile
    * @param   other         The other MultiBandTile
    * @param   method        The resampling method
    * @return                A new MultiBandTile, the result of the merge
    */
  def merge(extent: Extent, otherExtent: Extent, other: MultibandTile, method: ResampleMethod): MultibandTile = {
    val bands: Seq[Tile] =
      for {
        bandIndex <- 0 until self.bandCount
      } yield {
        val thisBand = self.band(bandIndex)
        val thatBand = other.band(bandIndex)
        thisBand.merge(extent, otherExtent, thatBand, method)
      }

    ArrayMultibandTile(bands)
  }
}
