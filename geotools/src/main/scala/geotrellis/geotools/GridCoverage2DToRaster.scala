/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.geotools

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.vector._

import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.gce.geotiff._
import org.osgeo.proj4j._

import scala.collection.JavaConverters._
import scala.math.{min, max}


/**
  * The [[GridCoverage2DToRaster]] object, contains a constructor, as
  * well as other functions for converting a GridCoverage2D to a
  * Geotrellis Raster.
  */
object GridCoverage2DToRaster {

  /**
    * This constructor takes a GridCoverage2D and returns a sequence
    * of Geotrellis Rasters.
    *
    * @param  gridCoverage  The  GeoTools GridCoverage2D object
    * @return               A sequence of rasters of singleband Geotrellis Tiles
    */
  def apply(gridCoverage: GridCoverage2D): Seq[Raster[Tile]] = {
    val extent = GridCoverage2DToRaster.extent(gridCoverage)

    (0 until bandCount(gridCoverage)).map({ i =>
      val tile = GridCoverage2DTile(gridCoverage, i)
      Raster(tile, extent)
    })
  }

  /**
    * This function extracts a Geotrellis Extent from the extent
    * information stored in the given GridCoverage2D.
    *
    * @param  gridCoverage  The GeoTools GridCoverage2D object
    */
  def extent(gridCoverage: GridCoverage2D): Extent = {
    val envelope = gridCoverage.getEnvelope
    val Array(xmin, ymin) = envelope.getUpperCorner.getCoordinate
    val Array(xmax, ymax) = envelope.getLowerCorner.getCoordinate

    Extent(min(xmin, xmax), min(ymin, ymax), max(xmin, xmax), max(ymin, ymax))
  }

  /**
    * This function extracts a Geotrellis CRS from the CRS information
    * stored in the given GridCoverage2D.
    *
    * @param  gridCoverage  The GeoTools GridCoverage2D object
    */
  def crs(gridCoverage: GridCoverage2D): Option[CRS] = {
    val crs = gridCoverage
      .getCoordinateReferenceSystem2D
      .getIdentifiers
      .asScala.head
    val code = crs.getCode.toInt
    val crsFactory = new CRSFactory

    var result: Option[CRS] = None
    try {
      require(crs.getCodeSpace.equals("EPSG"))
      result = Some(
        new CRS {
          val proj4jCrs = crsFactory.createFromName(s"EPSG:$code")
          def epsgCode: Option[Int] = Some(code)
        })
    } catch { case e: Exception => }

    result
  }

  /**
    * Get the number of bands present in a GridCoverage2D.
    *
    * @param  gridCoverage  The GeoTools GridCoverage2D object
    */
  def bandCount(gridCoverage: GridCoverage2D): Int = {
    val renderedImage = gridCoverage.getRenderedImage
    val buffer = renderedImage.getData.getDataBuffer
    val sampleModel = renderedImage.getSampleModel

    sampleModel.getNumBands
  }
}
