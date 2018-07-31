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

package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiffMultibandTile


/**
  * A trait containing crop methods for [[MultibandTile]]s.
  */
trait MultibandTileCropMethods extends TileCropMethods[MultibandTile] {
  import Crop.Options

  /**
    * Given a [[GridBounds]], a sequence of bands indexes,  and some cropping options, crop the
    * [[MultibandTile]] and return a new MultibandTile that contains the target area and bands.
    */
  def cropBands(gb: GridBounds, targetBands: Seq[Int], options: Options): MultibandTile = {
    if (!gb.intersects(self.gridBounds)) throw GeoAttrsError(s"Grid bounds do not intersect: ${self.gridBounds} crop $gb")
    self match {
      case geotiffTile: GeoTiffMultibandTile =>
        val cropBounds =
          if (options.clamp) gb.intersection(self).get
          else gb
        geotiffTile.crop(cropBounds, targetBands.toArray)
      case _ =>
        val croppedBands = Array.ofDim[Tile](targetBands.size)
        for (b <- targetBands) {
          croppedBands(b) = self.band(b).crop(gb, options)
        }
        ArrayMultibandTile(croppedBands)
    }
  }

  def cropBands(gridBounds: Seq[GridBounds], targetBands: Seq[Int], options: Options): Iterator[(GridBounds, MultibandTile)] =
    self match {
      case geotiffTile: GeoTiffMultibandTile =>
        val cropBounds: Seq[GridBounds] = gridBounds.map { gb =>
            if (!gb.intersects(self.gridBounds))
              throw GeoAttrsError(s"Grid bounds do not intersect: ${self.gridBounds} crop $gb")

            if (options.clamp) gb.intersection(self).get
            else gb
        }
        geotiffTile.crop(cropBounds, targetBands.toArray)
    }

  def cropBands(gridBounds: Seq[GridBounds], targetBands: Seq[Int]): Iterator[(GridBounds, MultibandTile)] =
    cropBands(gridBounds, targetBands, Options.DEFAULT)

  /**
   * Crops this [[MultibandTile]] to the given region using methods
   * specified in the cropping options.
   */
  def crop(gb: GridBounds, options: Options): MultibandTile =
    cropBands(gb, 0 until self.bandCount, options)

  /**
   * Crops this [[MultibandTile]] such that the output will contain
   * only the given region and bands specified.
   */
  def cropBands(gb: GridBounds, targetBands: Seq[Int]): MultibandTile =
    cropBands(gb, targetBands, Options.DEFAULT)


  /**
    * Given a source Extent (the extent of the present
    * [[MultibandTile]]), a destination Extent, and a set of Options,
    * return a new MultibandTile.
    */
  def crop(srcExtent: Extent, extent: Extent, options: Options): MultibandTile =
    Raster(self, srcExtent).crop(extent, options).tile
}
