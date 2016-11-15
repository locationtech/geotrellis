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

package geotrellis.geotools

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.util._
import geotrellis.vector.Extent

import org.geotools.coverage.grid._
import org.geotools.resources.coverage.CoverageUtilities
import spire.syntax.cfor._

import java.awt.image.{Raster => AwtRaster, _}

trait GridCoverage2DConversionMethods extends MethodExtensions[GridCoverage2D] {
  def toTile(bandIndex: Int): Tile =
    GridCoverage2DConverters.convertToTile(self, bandIndex)

  def toMultibandTile(): MultibandTile =
    GridCoverage2DConverters.convertToMultibandTile(self)

  def toRaster(bandIndex: Int): Raster[Tile] = {
    val tile = GridCoverage2DConverters.convertToTile(self, bandIndex)
    val extent = GridCoverage2DConverters.getExtent(self)

    Raster(tile, extent)
  }

  def toRaster(): Raster[MultibandTile] = {
    val renderedImage = self.getRenderedImage
    val sampleModel = renderedImage.getSampleModel
    val numBands = sampleModel.getNumBands

    val tile = {
      val tiles = Array.ofDim[Tile](numBands)
      cfor(0)(_ < numBands, _ + 1) { b =>
        tiles(b) = GridCoverage2DConverters.convertToTile(self, b)
      }
      ArrayMultibandTile(tiles)
    }
    val extent = GridCoverage2DConverters.getExtent(self)

    Raster(tile, extent)
  }

  def toProjectedRaster(bandIndex: Int): ProjectedRaster[Tile] =
    GridCoverage2DConverters.getCrs(self) match {
      case Some(crs) =>
        ProjectedRaster(toRaster(bandIndex), crs)
      case None =>
        // Default LatLng
        ProjectedRaster(toRaster(bandIndex), LatLng)
    }


  def toProjectedRaster(): ProjectedRaster[MultibandTile] =
    GridCoverage2DConverters.getCrs(self) match {
      case Some(crs) =>
        ProjectedRaster(toRaster(), crs)
      case None =>
        // Default LatLng
        ProjectedRaster(toRaster(), LatLng)
    }
}
