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

package geotrellis.raster.io.geotiff.writer

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._

import geotrellis.vector.Extent

import geotrellis.proj4.CRS
import geotrellis.proj4.LatLng

import geotrellis.testkit._

import org.scalatest._

class GeoTiffWriterSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with TestEngine
    with TileBuilders
    with GeoTiffTestUtils {

  override def afterAll = purge

  private val testProj4String =
    "+proj=lcc +lat_0=33.750000000 +lon_0=-79.000000000 +lat_1=36.166666667 +lat_2=34.333333333 +x_0=609601.220 +y_0=0.000 +units=m"

  private val testCRS = CRS.fromString(testProj4String)

  describe ("writing GeoTiffs without errors and with correct tiles, crs and extent") {

    it("should write GeoTiff with tags") {
      val path = "/tmp/geotiff-writer.tif"

      val geoTiff = MultiBandGeoTiff(geoTiffPath("multi-tag.tif"))
      GeoTiffWriter.write(geoTiff, path)

      addToPurge(path)

      val actual = MultiBandGeoTiff(path).tags
      val expected = geoTiff.tags

      actual should be (expected)
    }


    it ("should write floating point rasters correct") {
      val e = Extent(100.0, 400.0, 120.0, 420.0)
      val t = DoubleArrayTile(Array(11.0, 22.0, 33.0, 44.0), 2, 2)

      val geoTiff = SingleBandGeoTiff(t, e, testCRS, Tags.empty, GeoTiffOptions.DEFAULT)

      val path = "/tmp/geotiff-writer.tif"

      GeoTiffWriter.write(geoTiff, path)

      addToPurge(path)

      val SingleBandGeoTiff(tile, extent, crs, _) = SingleBandGeoTiff(path)

      extent should equal (e)
      crs should equal (testCRS)
      assertEqual(tile, t)
    }

    it ("should read write raster correctly") {
      val geoTiff = SingleBandGeoTiff.compressed(geoTiffPath("econic_zlib_tiled_bandint_wm.tif"))
      val projectedRaster = geoTiff.projectedRaster
      val ProjectedRaster(tile, extent, crs) = projectedRaster.reproject(LatLng)
      val reprojGeoTiff = SingleBandGeoTiff(tile, extent, crs, geoTiff.tags, geoTiff.options)

      val path = "/tmp/geotiff-writer.tif"

      GeoTiffWriter.write(reprojGeoTiff, path)

      addToPurge(path)

      val SingleBandGeoTiff(actualTile, actualExtent, actualCrs, _) = SingleBandGeoTiff(path)
      
      actualExtent should equal (extent)
      crs should equal (LatLng)
      assertEqual(actualTile, tile)
    }

    it ("should read write multibandraster correctly") {
      val geoTiff = MultiBandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif"))

      val path = "/Users/rob/tmp/geotiff-writer.tif"

      GeoTiffWriter.write(geoTiff, path)

      addToPurge(path)

      val gt = MultiBandGeoTiff(path)
      
      gt.extent should equal (geoTiff.extent)
      gt.crs should equal (geoTiff.crs)
      gt.tile.bandCount should equal (geoTiff.tile.bandCount)
      for(i <- 0 until gt.tile.bandCount) {
        val actualBand = gt.band(i)
        val expectedBand = geoTiff.band(i)

        assertEqual(actualBand, expectedBand)
      }
    }

    it ("should write hand made multiband and read back correctly") {
      val tile =
        ArrayMultiBandTile(
          positiveIntegerRaster,
          positiveIntegerRaster.map(_ * 100),
          positiveIntegerRaster.map(_ * 10000)
        )

      val geoTiff = MultiBandGeoTiff(tile, Extent(0.0, 0.0, 1000.0, 1000.0), LatLng)

      val path = "/Users/rob/tmp/geotiff-writer.tif"

      GeoTiffWriter.write(geoTiff, path)

      addToPurge(path)

      val gt = MultiBandGeoTiff(path)
      
      gt.extent should equal (geoTiff.extent)
      gt.crs should equal (geoTiff.crs)
      gt.tile.bandCount should equal (tile.bandCount)
      for(i <- 0 until gt.tile.bandCount) {
        val actualBand = gt.band(i)
        val expectedBand = tile.band(i)

        assertEqual(actualBand, expectedBand)
      }
    }

  }
}
