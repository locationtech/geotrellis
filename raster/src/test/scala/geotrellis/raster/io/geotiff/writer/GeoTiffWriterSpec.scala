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

package geotrellis.raster.io.geotiff.writer

import geotrellis.proj4.{CRS, LatLng}
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.raster.io.geotiff.tags.codes.ColorSpace
import geotrellis.raster.io.geotiff.writer.GeoTiffWriterSpec.tempPath
import geotrellis.raster.render.{ColorRamps, IndexedColorMap}
import geotrellis.raster.testkit._
import geotrellis.vector.Extent
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Inspectors}

import java.io._
import scala.xml.{Text, XML}

object GeoTiffWriterSpec {

  def tempPath(): String = {
    val tempFile: File = File.createTempFile("geotiff-writer", ".tif")
    tempFile.deleteOnExit()
    tempFile.getPath
  }
}

class GeoTiffWriterSpec extends AnyFunSpec with Matchers with BeforeAndAfterAll with RasterMatchers with TileBuilders with GeoTiffTestUtils {

  override def afterAll() = {
    println("done :)")
  }

  private val testCRS = CRS.fromName("EPSG:3857")
  private val testExtent = Extent(100.0, 400.0, 120.0, 420.0)

  describe("writing GeoTiffs without errors and with correct tiles, crs and extent") {
    it("should write GeoTiff with tags") {
      val geoTiff = MultibandGeoTiff(geoTiffPath("multi-tag.tif"))
      val path = tempPath()
      GeoTiffWriter.write(geoTiff, path)

      val tiffTags = TiffTags.read(path)
      val samples = tiffTags.nonBasicTags.extraSamples
      samples.get should equal(Array(0))

      val actual = MultibandGeoTiff(path).tags
      val expected = geoTiff.tags

      actual should be(expected)
    }

    it("should write singleband geotiffs (tiled)") {
      val tile = IntArrayTile.fill(7, 1000, 1000)
      val sbGeoTiffTiled = SinglebandGeoTiff(
        tile, Extent(0, 0, 1, 1), LatLng, Tags.empty,
        GeoTiffOptions.DEFAULT.copy(storageMethod = Tiled(256, 256))
      )
      val path = tempPath()
      GeoTiffWriter.write(sbGeoTiffTiled, path)
      val sbactualTiled = SinglebandGeoTiff(path)
      sbactualTiled.options.storageMethod shouldBe a[Tiled]
    }

    it("should write singleband geotiffs (striped)") {
      val tile = IntArrayTile.fill(7, 1000, 1000)
      val sbGeoTiffStriped = SinglebandGeoTiff(
        tile, Extent(0, 0, 1, 1), LatLng, Tags.empty,
        GeoTiffOptions.DEFAULT.copy(storageMethod = Striped())
      )
      val path = tempPath()
      GeoTiffWriter.write(sbGeoTiffStriped, path)
      val sbactualStriped = SinglebandGeoTiff(path)
      sbactualStriped.options.storageMethod shouldBe a[Striped]
    }

    it("should write multiband geotiffs (tiled)") {
      val tile = IntArrayTile.fill(7, 1000, 1000)
      val mbtile = ArrayMultibandTile(tile)
      val mbGeoTiffTiled = MultibandGeoTiff(
        mbtile, Extent(0, 0, 1, 1), LatLng, Tags.empty,
        GeoTiffOptions.DEFAULT.copy(storageMethod = Tiled(256, 256))
      )
      val path = tempPath()
      GeoTiffWriter.write(mbGeoTiffTiled, path)

      val tiffTags = TiffTags.read(path)
      val samples = tiffTags.nonBasicTags.extraSamples
      samples should equal(None)

      val mbactualTiled = MultibandGeoTiff(path)
      mbactualTiled.options.storageMethod shouldBe a[Tiled]
    }

    it("should write multiband geotiffs (striped)") {
      val tile = IntArrayTile.fill(7, 1000, 1000)
      val mbtile = ArrayMultibandTile(tile)
      val mbGeoTiffStriped = MultibandGeoTiff(
        mbtile, Extent(0, 0, 1, 1), LatLng, Tags.empty,
        GeoTiffOptions.DEFAULT.copy(storageMethod = Striped())
      )
      val path = tempPath()
      GeoTiffWriter.write(mbGeoTiffStriped, path)
      val mbactualStriped = MultibandGeoTiff(path)
      mbactualStriped.options.storageMethod shouldBe a[Striped]
    }

    it("should write GeoTiff with oversized custom tags") {
      val geoTiff = MultibandGeoTiff(geoTiffPath("multi-tag.tif"))

      val newTag1 = ("SOME_CUSTOM_TAG1" -> "1234567890123456789012345678901")
      val newTag2 = ("SOME_CUSTOM_TAG2" -> "12345678901234567890123456789012")
      val newTag3 = ("OFFSET" -> "43")
      val newTag4 = ("DESCRIPTION" -> "band description")
      val newTag5 = ("SCALE" -> "0.1")
      val headTags = geoTiff.tags.headTags + newTag1 + newTag2
      val bandTags = geoTiff.tags.bandTags.map(_ + newTag1 + newTag2 + newTag3 + newTag4 + newTag5)

      val taggedTiff = geoTiff.copy(tags = Tags(headTags, bandTags))

      val path = tempPath()
      GeoTiffWriter.write(taggedTiff, path)
      val tags = TiffTags.read(path)
      val samples = tags.nonBasicTags.extraSamples
      samples.get should equal(Array(0))

      val expectedXML = XML.loadString(
        """<GDALMetadata>
        <Item name="SOME_CUSTOM_TAG2">12345678901234567890123456789012</Item>
        <Item name="TAG_TYPE">HEAD</Item>
        <Item name="SOME_CUSTOM_TAG1">1234567890123456789012345678901</Item>
        <Item name="HEADTAG">1</Item>
        <Item name="SOME_CUSTOM_TAG2" sample="0">12345678901234567890123456789012</Item>
        <Item name="DESCRIPTION" sample="0" role="description">band description</Item>
        <Item name="TAG_TYPE" sample="0">BAND1</Item>
        <Item name="BANDTAG" sample="0">1</Item>
        <Item name="OFFSET" sample="0" role="offset">43</Item>
        <Item name="SOME_CUSTOM_TAG1" sample="0">1234567890123456789012345678901</Item>
        <Item name="SCALE" sample="0" role="scale">0.1</Item>
        <Item name="SOME_CUSTOM_TAG2" sample="1">12345678901234567890123456789012</Item>
        <Item name="DESCRIPTION" sample="1" role="description">band description</Item>
        <Item name="TAG_TYPE" sample="1">BAND2</Item>
        <Item name="BANDTAG" sample="1">2</Item>
        <Item name="OFFSET" sample="1" role="offset">43</Item>
        <Item name="SOME_CUSTOM_TAG1" sample="1">1234567890123456789012345678901</Item>
        <Item name="SCALE" sample="1" role="scale">0.1</Item>
        <Item name="SOME_CUSTOM_TAG2" sample="2">12345678901234567890123456789012</Item>
        <Item name="DESCRIPTION" sample="2" role="description">band description</Item>
        <Item name="TAG_TYPE" sample="2">BAND3</Item>
        <Item name="BANDTAG" sample="2">3</Item>
        <Item name="OFFSET" sample="2" role="offset">43</Item>
        <Item name="SOME_CUSTOM_TAG1" sample="2">1234567890123456789012345678901</Item>
        <Item name="SCALE" sample="2" role="scale">0.1</Item>
        <Item name="SOME_CUSTOM_TAG2" sample="3">12345678901234567890123456789012</Item>
        <Item name="DESCRIPTION" sample="3" role="description">band description</Item>
        <Item name="TAG_TYPE" sample="3">BAND4</Item>
        <Item name="BANDTAG" sample="3">4</Item>
        <Item name="OFFSET" sample="3" role="offset">43</Item>
        <Item name="SOME_CUSTOM_TAG1" sample="3">1234567890123456789012345678901</Item>
        <Item name="SCALE" sample="3" role="scale">0.1</Item>
      </GDALMetadata>""")
      val expectedItems = expectedXML.child.filter(_.isInstanceOf[scala.xml.Elem]).sortBy(_.attribute("name").get.text).sortBy(_.attribute("sample").getOrElse(Text("")).text)
      val actualItems = XML.loadString(tags.geoTiffTags.metadata.get).child.filter(_.isInstanceOf[scala.xml.Elem]).sortBy(_.attribute("name").get.text).sortBy(_.attribute("sample").getOrElse(Text("")).text)

      actualItems.zip(expectedItems).foreach(t => t._1 should equal(t._2))

      val actual = MultibandGeoTiff(path).tags
      val expected = taggedTiff.tags

      actual should be(expected)
    }

    it("should write web mercator correctly") {
      val geoTiff = SinglebandGeoTiff(geoTiffPath("ndvi-web-mercator.tif"))

      val path = tempPath()
      geoTiff.write(path)
      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.epsgCode should be(geoTiff.crs.epsgCode)
    }

    it("should write NY State Plane correctly") {
      val geoTiff = SinglebandGeoTiff(geoTiffPath("ny-state-plane.tif"))

      val path = tempPath()
      geoTiff.write(path)
      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.epsgCode should be(geoTiff.crs.epsgCode)
    }

    it("should write Polar stereographic correctly") {
      val geoTiff = SinglebandGeoTiff(geoTiffPath("alaska-polar-3572.tif"))
      val path = tempPath()
      geoTiff.write(path)
      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.epsgCode should be(geoTiff.crs.epsgCode)
    }

    it("should write Sinusoidal correctly") {

      val geoTiff = SinglebandGeoTiff(geoTiffPath("reproject/modis_sinu.tif"))

      val path = tempPath()
      geoTiff.write(path)
      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.toProj4String should be(geoTiff.crs.toProj4String)
    }

    it("should write Albers Equal Area correctly") {

      val geoTiff = SinglebandGeoTiff(geoTiffPath("reproject/alaska-aea.tif"))

      val path = tempPath()
      geoTiff.write(path)
      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.toProj4String should be(geoTiff.crs.toProj4String)
    }

    it("should write Lambert Conformal Conic correctly") {

      val geoTiff = SinglebandGeoTiff(geoTiffPath("reproject/lcc.tif"))

      val path = tempPath()
      geoTiff.write(path)
      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.toProj4String should be(geoTiff.crs.toProj4String)
    }

    it("should write DHDN_3_Degree_Gauss_Zone_3 correctly") {
      val geoTiff = MultibandGeoTiff(geoTiffPath("epsg31467.tif"))
      val originalTiffTags = TiffTags.read(geoTiffPath("epsg31467.tif"))
      val originalSamples = originalTiffTags.nonBasicTags.extraSamples
      originalSamples.get should equal(Array(2))

      val path = tempPath()
      geoTiff.write(path)
      val tiffTags = TiffTags.read(path)
      val samples = tiffTags.nonBasicTags.extraSamples
      samples.get should equal(Array(2))

      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.toProj4String should be(geoTiff.crs.toProj4String)
    }

    it("should write web mercator with no epsg code correctly") {
      val geoTiff = MultibandGeoTiff(geoTiffPath("ndvi-web-mercator.tif"))

      val path = tempPath()
      GeoTiff(geoTiff.raster, CRS.fromString(geoTiff.crs.toProj4String)).write(path)
      val actualCRS = SinglebandGeoTiff(path).crs

      actualCRS.toProj4String should be(geoTiff.crs.toProj4String)
    }

    it("should write floating point rasters correctly") {
      val t = DoubleArrayTile(Array(11.0, 22.0, 33.0, 44.0), 2, 2)

      val geoTiff = SinglebandGeoTiff(t, testExtent, testCRS, Tags.empty, GeoTiffOptions.DEFAULT)
      val path = tempPath()
      GeoTiffWriter.write(geoTiff, path)

      val SinglebandGeoTiff(tile, extent, crs, _, _, _) = SinglebandGeoTiff(path)

      extent should equal(testExtent)
      crs should equal(testCRS)
      assertEqual(tile, t)
    }

    it("should read write raster correctly") {
      val geoTiff = SinglebandGeoTiff(geoTiffPath("econic_zlib_tiled_bandint_wm.tif"))
      val projectedRaster = geoTiff.projectedRaster
      val ProjectedRaster(Raster(tile, extent), crs) = projectedRaster.reproject(LatLng)
      val reprojGeoTiff = SinglebandGeoTiff(tile, extent, crs, geoTiff.tags, geoTiff.options)

      val path = tempPath()
      GeoTiffWriter.write(reprojGeoTiff, path)

      val SinglebandGeoTiff(actualTile, actualExtent, actualCrs, _, _, _) = SinglebandGeoTiff(path)

      actualExtent should equal(extent)
      crs should equal(LatLng)
      assertEqual(actualTile, tile)
    }

    it("should read write multibandraster correctly") {
      val geoTiff = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif"))

      val path = tempPath()
      GeoTiffWriter.write(geoTiff, path)

      val gt = MultibandGeoTiff(path)

      gt.extent should equal(geoTiff.extent)
      gt.crs should equal(geoTiff.crs)
      gt.tile.bandCount should equal(geoTiff.tile.bandCount)
      for (i <- 0 until gt.tile.bandCount) {
        val actualBand = gt.tile.band(i)
        val expectedBand = geoTiff.tile.band(i)

        assertEqual(actualBand, expectedBand)
      }
    }

    it("should read write multibandraster with compression correctly") {
      val geoTiff = {
        val gt = MultibandGeoTiff(geoTiffPath("3bands/int32/3bands-striped-pixel.tif"))
        MultibandGeoTiff(Raster(gt.raster.tile.toArrayTile(), gt.raster.extent), gt.crs, options = GeoTiffOptions(compression.DeflateCompression))
      }

      val path = tempPath()
      GeoTiffWriter.write(geoTiff, path)

      val tags = TiffTags.read(path)
      tags.compression should be(geotrellis.raster.io.geotiff.tags.codes.CompressionType.ZLibCoded)

      val gt = MultibandGeoTiff(path)

      gt.extent should equal(geoTiff.extent)
      gt.crs should equal(geoTiff.crs)
      gt.tile.bandCount should equal(geoTiff.tile.bandCount)
      for (i <- 0 until gt.tile.bandCount) {
        val actualBand = gt.tile.band(i)
        val expectedBand = geoTiff.tile.band(i)

        assertEqual(actualBand, expectedBand)
      }
    }

    it("should write hand made multiband and read back correctly") {
      val tile =
        ArrayMultibandTile(
          positiveIntegerRaster,
          positiveIntegerRaster.map(_ * 100),
          positiveIntegerRaster.map(_ * 10000)
        )

      val geoTiff = MultibandGeoTiff(tile, Extent(0.0, 0.0, 1000.0, 1000.0), LatLng)
      val path = tempPath()
      GeoTiffWriter.write(geoTiff, path)

      val gt = MultibandGeoTiff(path).projectedRaster

      gt.extent should equal(geoTiff.extent)
      gt.crs should equal(geoTiff.crs)
      gt.tile.bandCount should equal(tile.bandCount)
      for (i <- 0 until gt.tile.bandCount) {
        val actualBand = gt.tile.band(i)
        val expectedBand = tile.band(i)

        assertEqual(actualBand, expectedBand)
      }
    }

    it("should write a GeoTiff to byte array") {
      val tile =
        ArrayMultibandTile(
          positiveIntegerRaster,
          positiveIntegerRaster.map(_ * 100),
          positiveIntegerRaster.map(_ * 10000)
        )

      val geoTiff = MultibandGeoTiff(tile, Extent(0.0, 0.0, 1000.0, 1000.0), LatLng)

      val bytes = GeoTiffWriter.write(geoTiff)

      val gt = MultibandGeoTiff(bytes)

      gt.extent should equal(geoTiff.extent)
      gt.crs should equal(geoTiff.crs)
      gt.tile.bandCount should equal(tile.bandCount)
      for (i <- 0 until gt.tile.bandCount) {
        val actualBand = gt.tile.band(i)
        val expectedBand = tile.band(i)

        assertEqual(actualBand, expectedBand)
      }
    }

    it("should write a MultibandGeoTiff with overviews correct") {
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiffOriginal = MultibandGeoTiff(geoTiffPath("overviews/multiband.tif"))

      val path = tempPath()
      tiffOriginal.write(path)

      val tiff = MultibandGeoTiff(path)
      val tile = tiff.tile

      tiff.getOverviewsCount should be(5)
      tile.bandCount should be(4)
      tile.bands.map(_.isNoDataTile).reduce(_ && _) should be(false)

      tile.cols -> tile.rows should be(sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.getOverviewsCount should be(0)
        ovrTile.bandCount should be(4)
        ovrTile.bands.map(_.isNoDataTile).reduce(_ && _) should be(false)

        ovrTile.cols -> ovrTile.rows should be(ovrSize)
      }
    }

    it("should write a striped MultibandGeoTiff with overviews as tiled cloud optimized 128x128 tiff") {
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      // Reading not cloud optimized tiff
      val tiffOriginal = MultibandGeoTiff(geoTiffPath("overviews/multiband.tif"))

      (tiffOriginal.options.storageMethod match {
        case _: Striped => true
        case _: Tiled => false
      }) should be(true)

      val tiledTiff = tiffOriginal.withStorageMethod(Tiled(128, 128))

      val path = tempPath()
      tiledTiff.write(path, true)

      val tiff = MultibandGeoTiff(path)
      val tile = tiff.tile

      tiff.options.storageMethod should be(Tiled(128, 128))
      tiff.getOverviewsCount should be(5)
      tile.bandCount should be(4)
      tile.bands.map(_.isNoDataTile).reduce(_ && _) should be(false)

      tile.cols -> tile.rows should be(sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.options.storageMethod should be(Tiled(128, 128))
        ovrTiff.getOverviewsCount should be(0)
        ovrTile.bandCount should be(4)
        ovrTile.bands.map(_.isNoDataTile).reduce(_ && _) should be(false)

        ovrTile.cols -> ovrTile.rows should be(ovrSize)
      }
    }

    it("should write a striped SinglebandGeoTiff with overviews as tiled cloud optimized 128x128 tiff") {
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      // Reading not cloud optimized tiff
      val tiffOriginal = SinglebandGeoTiff(geoTiffPath("overviews/singleband.tif"))

      (tiffOriginal.options.storageMethod match {
        case _: Striped => true
        case _: Tiled => false
      }) should be(true)

      val tiledTiff = tiffOriginal.withStorageMethod(Tiled(128, 128))

      val path = tempPath()
      tiledTiff.write(path, true)

      val tiff = SinglebandGeoTiff(path)
      val tile = tiff.tile

      tiff.options.storageMethod should be(Tiled(128, 128))
      tiff.getOverviewsCount should be(5)
      tile.isNoDataTile should be(false)

      tile.cols -> tile.rows should be(sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.options.storageMethod should be(Tiled(128, 128))
        ovrTiff.getOverviewsCount should be(0)
        ovrTile.isNoDataTile should be(false)

        ovrTile.cols -> ovrTile.rows should be(ovrSize)
      }
    }

    it("should write a SinglebandGeoTiff with overviews correct") {
      // sizes of overviews, starting with the base ifd
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiffOriginal = SinglebandGeoTiff(geoTiffPath("overviews/singleband.tif"))
      val path = tempPath()
      tiffOriginal.write(path)

      val tiff = SinglebandGeoTiff(path)
      val tile = tiff.tile

      tiff.getOverviewsCount should be(5)
      tile.isNoDataTile should be(false)

      tile.cols -> tile.rows should be(sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.getOverviewsCount should be(0)
        ovrTile.isNoDataTile should be(false)

        ovrTile.cols -> ovrTile.rows should be(ovrSize)
      }
    }

    it("should write cloud optimized MultibandGeoTiff with overviews correct") {
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiffOriginal = MultibandGeoTiff(geoTiffPath("overviews/multiband_co.tif"))
      val path = tempPath()
      tiffOriginal.write(path, true)

      val tiff = MultibandGeoTiff(path)
      val tile = tiff.tile

      tiff.getOverviewsCount should be(5)
      tile.bandCount should be(4)
      tile.bands.map(_.isNoDataTile).reduce(_ && _) should be(false)

      tile.cols -> tile.rows should be(sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.getOverviewsCount should be(0)
        ovrTile.bandCount should be(4)
        ovrTile.bands.map(_.isNoDataTile).reduce(_ && _) should be(false)

        ovrTile.cols -> ovrTile.rows should be(ovrSize)
      }
    }

    it("should write cloud optimized SinglebandGeoTiff with overviews correct") {
      // sizes of overviews, starting with the base ifd
      val sizes = List(1056 -> 1052, 528 -> 526, 264 -> 263, 132 -> 132, 66 -> 66, 33 -> 33)

      val tiffOriginal = SinglebandGeoTiff(geoTiffPath("overviews/singleband_co.tif"))
      val path = tempPath()
      tiffOriginal.write(path, true)

      val tiff = SinglebandGeoTiff(path)
      val tile = tiff.tile

      tiff.getOverviewsCount should be(5)
      tile.isNoDataTile should be(false)

      tile.cols -> tile.rows should be(sizes(0))

      tiff.overviews.zip(sizes.tail).foreach { case (ovrTiff, ovrSize) =>
        val ovrTile = ovrTiff.tile

        ovrTiff.getOverviewsCount should be(0)
        ovrTile.isNoDataTile should be(false)

        ovrTile.cols -> ovrTile.rows should be(ovrSize)
      }
    }
  }
  describe("writing GeoTiffs with correct color handling") {

    it("should write photometric interpretation code") {
      // Read in a 4-band file interpreted as RGB(A)
      val base = MultibandGeoTiff(geoTiffPath("multi-tag.tif"))

      base.options.colorSpace should be(ColorSpace.RGB)

      val modifiedOptions = base.options.copy(colorSpace = ColorSpace.CMYK)

      val modifiedImage = base.copy(options = modifiedOptions)

      val path = tempPath()
      GeoTiffWriter.write(modifiedImage, path)

      val reread = MultibandGeoTiff(path)

      reread.options.colorSpace should be(ColorSpace.CMYK)
    }

    it("should write color map when photometric interpretation is 'Palette'") {
      val hundreds = createConsecutiveTile(10).map(_ - 1).convert(ByteCellType)

      val colorMap = ColorRamps.HeatmapBlueToYellowToRedSpectrum
        .stops(100)
        .toColorMap(Array.tabulate[Int](100)(identity))
      val indexedColorMap = IndexedColorMap.fromColorMap(colorMap)

      val indexed = SinglebandGeoTiff(hundreds, testExtent, testCRS, Tags.empty, GeoTiffOptions(indexedColorMap))

      val path = tempPath()
      GeoTiffWriter.write(indexed, path)

      val reread = MultibandGeoTiff(path)

      reread.options.colorSpace should be(ColorSpace.Palette)
      reread.options.colorMap should be(Symbol("defined"))

      val p1 = reread.options.colorMap.get.colors
      val p2 = indexed.options.colorMap.get.colors

      Inspectors.forEvery(p1.zip(p2)) { case (c1, c2) =>
        c1 should equal(c2)
      }
    }

    it("should preserve color map in existing file") {
      val base = SinglebandGeoTiff(geoTiffPath("colormap.tif"))
      val path = tempPath()
      GeoTiffWriter.write(base, path)

      val reread = MultibandGeoTiff(path)

      val p1 = reread.options.colorMap.get.colors
      val p2 = base.options.colorMap.get.colors

      Inspectors.forEvery(p1.zip(p2)) { case (c1, c2) =>
        c1 should equal(c2)
      }
    }

    it("should inhibit writing unsupported 'Palette' color map configuration") {
      val base = SinglebandGeoTiff(geoTiffPath("colormap.tif"))
      val illegal = Seq(FloatCellType, DoubleCellType, IntCellType)

      Inspectors.forEvery(illegal) { cellType =>
        val naughty = base.copy(tile = base.tile.convert(cellType))
        val path = tempPath()
        intercept[IncompatibleGeoTiffOptionsException] {
          GeoTiffWriter.write(naughty, path)
        }
      }
    }
  }
}
