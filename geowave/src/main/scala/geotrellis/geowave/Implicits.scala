/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave

import java.util.function.Supplier

import geotrellis.proj4.CRS
import geotrellis.raster.{CellGrid, Dimensions, Raster, RasterExtent}
import geotrellis.util.MethodExtensions
import geotrellis.vector.Extent
import org.opengis.referencing.crs.CoordinateReferenceSystem
import spire.math._
import spire.syntax.convertableFrom._

trait Implicits {
  /** An implicit that converts object constructor into [[Supplier]] */
  implicit def supplier[T](f: () => T): Supplier[T] = { () => f() }

  /** Converts GeoTrellis [[CRS]] object to GeoTools [[CoordinateReferenceSystem]]  */
  implicit def geotrellisCRSToGeoToolsCoordinateReferenceSystem(crs: CRS): CoordinateReferenceSystem =
    crs.epsgCode match {
      case Some(code) => org.geotools.referencing.CRS.decode(s"EPSG:${code}")
      case _ => crs.toWKT() match {
        case Some(wkt) => org.geotools.referencing.CRS.parseWKT(wkt)
        case _ => throw new Exception("Can't convert GeoTrellis CRS into GeoTools CRS")
      }
    }

  /** Method extensions to normalize [[geotrellis.proj4.LatLng]] [[Extent]] */
  implicit class extentLatLngMethodExtensions(val self: Extent) extends MethodExtensions[Extent] {
    // Y // [-90; +90]
    private def clampLat(lat: Double): Double = if (lat < -90) -90 else if (lat > 90) 90 else lat
    // X // [-180; +180]
    private def clampLng(lng: Double): Double = if (lng < -180) -180 else if (lng > 180) 180 else lng

    // https://github.com/locationtech/proj4j/blob/v1.1.0/src/main/java/org/locationtech/proj4j/proj/Projection.java#L795-L803
    private def normalizeLng(lng: Double): Double = {
      var angle = lng
      while (angle > 180) angle -= 360
      while (angle < -180) angle += 360
      angle
    }

    /** WARN: can be used only with [[geotrellis.proj4.LatLng]] only */
    def clampLatLng: Extent = {
      Extent(
        xmin = clampLng(self.xmin),
        xmax = clampLng(self.xmax),
        ymin = clampLat(self.ymin),
        ymax = clampLat(self.ymax)
      )
    }

    /** WARN: can be used only with [[geotrellis.proj4.LatLng]] only */
    def normalizeLatLng: Extent = {
      val List(xmin, xmax) = List(normalizeLng(self.xmin), normalizeLng(self.xmax)).sorted

      Extent(
        xmin = xmin,
        xmax = xmax,
        ymin = self.ymin,
        ymax = self.ymax
      )
    }
  }

  /** Method extensions to normalize [[geotrellis.proj4.LatLng]] [[RasterExtent]] */
  implicit class rasterExtentLatLngMethodExtensions(val self: RasterExtent) extends MethodExtensions[RasterExtent] {
    /** WARN: can be used only with [[geotrellis.proj4.LatLng]] only */
    def normalizeLatLng: RasterExtent = RasterExtent(self.extent.normalizeLatLng, self.cols, self.rows)
  }

  /** Method extensions to normalize [[geotrellis.proj4.LatLng]] [[Extent]] */
  implicit class rasterLatLngMethodExtensions[T <: CellGrid[Int]](val self: Raster[T]) extends MethodExtensions[Raster[T]] {
    /** WARN: can be used only with [[geotrellis.proj4.LatLng]] only */
    def normalizeLatLngExtent: Raster[T] = Raster(self.tile, self.extent.normalizeLatLng)
  }
}

object Implicits extends Implicits
