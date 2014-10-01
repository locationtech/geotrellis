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

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS
import geotrellis.proj4.LatLng

import geotrellis.testkit._

import org.scalatest._

class GeoTiffWriterSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with TestEngine
    with GeoTiffTestUtils {

  override def afterAll = purge

  private val testProj4String =
    "+proj=lcc +lat_0=33.750000000 +lon_0=-79.000000000 +lat_1=36.166666667 +lat_2=34.333333333 +x_0=609601.220 +y_0=0.000 +units=m"

  private val testCRS = CRS.fromString(testProj4String)

  describe ("writing GeoTiffs without errors and with correct tiles, crs and extent") {

    it ("should write floating point rasters correct") {
      val e = Extent(100.0, 400.0, 120.0, 420.0)
      val r = DoubleArrayTile(Array(11.0, 22.0, 33.0, 44.0), 2, 2)

      val path = "/tmp/float.tif"

      GeoTiffWriter.write(path, r, e, testCRS)

      addToPurge(path)

      val ifd = GeoTiffReader(path).read.imageDirectories.head

      val (raster, extent, crs) = ifd.toRaster

      extent should equal (e)
      raster should equal (r)
      crs should equal (testCRS)
    }

    it ("should write floating point rasters with the default LatLng CRS correctly") {
      val e = Extent(100.0, 400.0, 120.0, 420.0)
      val r = DoubleArrayTile(Array(11.0, 22.0, 33.0, 44.0), 2, 2)

      val path = "/tmp/latlng.tif"

      GeoTiffWriter.write(path, r, e, LatLng)

      addToPurge(path)

      val ifd = GeoTiffReader(path).read.imageDirectories.head

      val (raster, extent, crs) = ifd.toRaster

      extent should equal (e)
      raster should equal (r)
      crs should equal (LatLng)
    }

  }

}
