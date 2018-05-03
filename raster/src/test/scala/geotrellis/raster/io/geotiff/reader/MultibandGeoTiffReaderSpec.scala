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

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._

import org.scalatest._

class MultibandGeoTiffReaderSpec extends FunSpec
    with RasterMatchers
    with GeoTiffTestUtils {

  describe("Reading geotiffs with INTERLEAVE=PIXEL") {
    it("Uncompressed, Stripped") {

      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-striped-pixel.tif")).tile

      // println("         PIXEL UNCOMPRESSED STRIPPED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("Uncompressed, Tiled") {
      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-tiled-pixel.tif")).tile

      // println("         PIXEL UNCOMPRESSED TILED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { (col, row, z) => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("COMPRESSION=DEFLATE, Stripped") {
      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/3bands-deflate.tif")).tile

      // println("         PIXEL COMPRESSED STRIPPED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("COMPRESSION=DEFLATE, Tiled") {
      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/3bands-tiled-deflate.tif")).tile

      // println("         PIXEL COMPRESSED TILED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("should read tiff with overviews correct") {
      // sizes of overviews, starting with the base ifd
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiff = MultibandGeoTiff(geoTiffPath("overviews/multiband.tif"))
      val tile = tiff.tile

      tiff.getOverviewsCount should be (5)
      tile.bandCount should be (4)
      tile.bands.map(_.isNoDataTile).reduce(_ && _) should be (false)

      tile.cols -> tile.rows should be (sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.getOverviewsCount should be (0)
        ovrTile.bandCount should be (4)
        ovrTile.bands.map(_.isNoDataTile).reduce(_ && _) should be (false)

        ovrTile.cols -> ovrTile.rows should be (ovrSize)
      }
    }

    it("should read tiff with external overviews correct") {
      // sizes of overviews, starting with the base ifd
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiff = MultibandGeoTiff(geoTiffPath("overviews/multiband_ext.tif"))
      val tile = tiff.tile

      tiff.getOverviewsCount should be (5)
      tile.bandCount should be (4)
      tile.bands.map(_.isNoDataTile).reduce(_ && _) should be (false)

      tile.cols -> tile.rows should be (sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.getOverviewsCount should be (0)
        ovrTile.bandCount should be (4)
        ovrTile.bands.map(_.isNoDataTile).reduce(_ && _) should be (false)

        ovrTile.cols -> ovrTile.rows should be (ovrSize)
      }
    }

    it("should read bigtiff with overviews correct") {
      // sizes of overviews, starting with the base ifd
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiff = MultibandGeoTiff(geoTiffPath("overviews/big_multiband.tif"))

      val tile = tiff.tile

      tiff.getOverviewsCount should be (5)
      tile.bandCount should be (4)
      tile.bands.map(_.isNoDataTile).reduce(_ && _) should be (false)

      tile.cols -> tile.rows should be (sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.getOverviewsCount should be (0)
        ovrTile.bandCount should be (4)
        ovrTile.bands.map(_.isNoDataTile).reduce(_ && _) should be (false)

        ovrTile.cols -> ovrTile.rows should be (ovrSize)
      }
    }

    it("should crop tiff with overviews correct choosing the best matching overview") {
      // sizes of overviews, starting with the base ifd
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiff = MultibandGeoTiff(geoTiffPath("overviews/multiband.tif"))

      // should grab the second overview
      val ctiff = tiff.crop(tiff.extent, CellSize(tiff.extent, sizes(2)))
      val otiff = tiff.overviews(1)

      ctiff.rasterExtent should be (RasterExtent(tiff.extent, CellSize(tiff.extent, otiff.tile.dimensions)))

      val (ctile, otile) = ctiff.tile -> otiff.tile

      ctile.bandCount should be (otile.bandCount)
      ctile.bands.map(_.isNoDataTile).reduce(_ && _) should be (otile.bands.map(_.isNoDataTile).reduce(_ && _))

      ctile.cols -> ctile.rows should be (otile.cols -> otile.rows)
    }
  }

  describe("Reading geotiffs with INTERLEAVE=BANDS") {
    it("Uncompressed, Stripped") {
      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-striped-band.tif")).tile


      // println("         PIXEL UNCOMPRESSED STRIPPED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("Uncompressed, Tiled") {
      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/int32/3bands-tiled-band.tif")).tile

      // println("         BANDS UNCOMPRESSED TILED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("COMPRESSION=DEFLATE, Stripped") {
      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/3bands-interleave-bands-deflate.tif")).tile

      // println("         BANDS COMPRESSED STRIPPED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }

    it("COMPRESSION=DEFLATE, Tiled") {
      val tile =
        reader.GeoTiffReader.readMultiband(geoTiffPath("3bands/3bands-tiled-interleave-bands-deflate.tif")).tile

      // println("         BANDS COMPRESSED TILED")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (2) }
      tile.band(2).foreach { z => z should be (3) }
    }
  }

  describe("reading BIT multiband rasters") {
    def p(s: String, i: String): String =
      geoTiffPath(s"3bands/bit/3bands-${s}-${i}.tif")

    it("should read pixel interleave, striped") {
      val tile: GeoTiffMultibandTile =
        MultibandGeoTiff(
          path = p("striped", "pixel"),
          streaming = false
        ).tile.asInstanceOf[GeoTiffMultibandTile]

      // println("         BIT BANDS")
      // println(tile.band(0).asciiDraw)
      // println(tile.band(1).asciiDraw)
      // println(tile.band(2).asciiDraw)

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (0) }
      tile.band(2).foreach { z => z should be (1) }
    }

    it("should read pixel interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "pixel")).tile

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (0) }
      tile.band(2).foreach { z => z should be (1) }
    }

    it("should read band interleave, striped") {
      val tile =
        MultibandGeoTiff(p("striped", "band")).tile

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (0) }
      tile.band(2).foreach { z => z should be (1) }
    }

    it("should read band interleave, tiled") {
      val tile =
        MultibandGeoTiff(p("tiled", "band")).tile

      tile.band(0).foreach { z => z should be (1) }
      tile.band(1).foreach { z => z should be (0) }
      tile.band(2).foreach { z => z should be (1) }
    }
  }
}
