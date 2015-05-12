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

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.arg._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.op.zonal.summary._

import geotrellis.vector.Extent
import geotrellis.testkit._
import geotrellis.proj4.{CRS, LatLng}

import monocle.syntax._

import scala.io.{Source, Codec}
import scala.collection.immutable.HashMap

import java.util.BitSet
import java.nio.ByteBuffer

import spire.syntax.cfor._
import org.scalatest._

class GeoTiffReaderSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with TestEngine
    with GeoTiffTestUtils {

  override def afterAll = purge

  describe("reading an ESRI generated Float32 geotiff with 0 NoData value") {

    it("matches an arg produced from geotrellis.gdal reader of that tif") {
      val tile = SingleBandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri.tif")).tile

      val expectedTile =
        ArgReader.read(geoTiffPath("us_ext_clip_esri.json")).tile

      assertEqual(tile, expectedTile)
    }

  }

  describe("reading slope.tif") {

    it("should match the ARG version") {
      val path = "slope.tif"
      val argPath = s"$baseDataPath/data/slope.json"

      val tile = SingleBandGeoTiff.compressed(s"$baseDataPath/$path").tile

      val expectedTile =
        ArgReader.read(argPath).tile

      assertEqual(tile, expectedTile)
    }

  }

  describe("reading compressed file must yield same image array as uncompressed file") {

    it("must read econic_lzw.tif and match uncompressed file") {
      val decomp = SingleBandGeoTiff.compressed(geoTiffPath("econic_lzw.tif"))
      val uncomp = SingleBandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read econic_zlib.tif and match uncompressed file") {
      val decomp = SingleBandGeoTiff.compressed(geoTiffPath("econic_zlib.tif"))
      val uncomp = SingleBandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read econic_zlib_tiled.tif and match uncompressed file") {
      val decomp = SingleBandGeoTiff.compressed(geoTiffPath("econic_zlib_tiled.tif"))
      val uncomp = SingleBandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read econic_zlib_tiled_bandint.tif and match uncompressed file") {
      val decomp = SingleBandGeoTiff.compressed(geoTiffPath("econic_zlib_tiled_bandint.tif"))
      val uncomp = SingleBandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read all-ones.tif and match uncompressed file") {
      val decomp = SingleBandGeoTiff.compressed(geoTiffPath("all-ones.tif"))
      val uncomp = SingleBandGeoTiff.compressed(geoTiffPath("all-ones-no-comp.tif"))

      assertEqual(decomp.tile, uncomp.tile)
    }
  }

  describe("reading tiled file must yield same image as strip files") {

    it("must read bilevel_tiled.tif and match strip file") {
      val tiled = SingleBandGeoTiff.compressed(geoTiffPath("bilevel_tiled.tif"))
      val striped = SingleBandGeoTiff.compressed(geoTiffPath("bilevel.tif"))

      assertEqual(tiled.tile, striped.tile)
    }

    it("must read us_ext_clip_esri.tif and match strip file") {
      val tiled = SingleBandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri.tif"))
      val striped = SingleBandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri_stripes.tif"))

      assertEqual(tiled.tile, striped.tile)
    }

  }

  describe("match tiff tags and geokeys correctly") {

    it("must match aspect.tif tiff tags") {
      val tiffTags = TiffTagsReader.read(s"$baseDataPath/aspect.tif")

      tiffTags.cols should equal (1500L)

      tiffTags.rows should equal (1350L)

      tiffTags.bitsPerSample should be (32)

      tiffTags.compression should equal (1)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._photometricInterp get) should equal (1)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._stripOffsets get) match {
        case Some(stripOffsets) => stripOffsets.size should equal (1350)
        case None => fail
      }

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._samplesPerPixel get) should equal (1)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._rowsPerStrip get) should equal (1L)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._stripByteCounts get) match {
        case Some(stripByteCounts) => stripByteCounts.size should equal (1350)
        case None => fail
      }

      (tiffTags &|-> TiffTags._nonBasicTags ^|->
        NonBasicTags._planarConfiguration get) match {
        case Some(planarConfiguration) => planarConfiguration should equal (1)
        case None => fail
      }

      val sampleFormat = 
        (tiffTags 
          &|-> TiffTags._dataSampleFormatTags
          ^|-> DataSampleFormatTags._sampleFormat get)
      sampleFormat should be (3)

      (tiffTags &|-> TiffTags._geoTiffTags
        ^|-> GeoTiffTags._modelPixelScale get) match {
        case Some(modelPixelScales) => {
          modelPixelScales._1 should equal (10.0)
          modelPixelScales._2 should equal (10.0)
          modelPixelScales._3 should equal (0.0)
        }
        case None => fail
      }

      (tiffTags &|-> TiffTags._geoTiffTags
        ^|-> GeoTiffTags._modelTiePoints get) match {
        case Some(modelTiePoints) if (modelTiePoints.size == 1) => {
          val (p1, p2) = modelTiePoints(0)
          p1.x should equal (0.0)
          p1.y should equal (0.0)
          p1.z should equal (0.0)
          p2.x should equal (630000.0)
          p2.y should equal (228500.0)
          p2.z should equal (0.0)
        }
        case None => fail
      }

      (tiffTags &|-> TiffTags._geoTiffTags
        ^|-> GeoTiffTags._gdalInternalNoData get) match {
        case Some(gdalInternalNoData) => gdalInternalNoData should equal (-9999.0)
        case None => fail
      }
    }

    it("must match aspect.tif geokeys") {
      val tiffTags = TiffTagsReader.read(s"$baseDataPath/aspect.tif")

      tiffTags.hasPixelArea should be (true)

      val extent = tiffTags.extent

      val minX = extent.xmin should equal (630000.0)
      val minY = extent.ymin should equal (215000.0)
      val maxX = extent.xmax should equal (645000.0)
      val maxY = extent.ymax should equal (228500.0)

      tiffTags.bandType.cellType should equal (TypeFloat)
    }

    // TODO: Deal with color maps.
  //   it("must match colormap.tif colormap") {
  //     val tags = GeoTiffReader
  //       .read(geoTiffPath("colormap.tif")
  //       .imageDirectory

  //     val colorMap = (tags &|->
  //       Tags._basicTags ^|->
  //       BasicTags._colorMap get)

  //     val nonCommonsMap = collection.immutable.HashMap[Int, (Byte, Byte, Byte)](
  //       1 -> (0.toByte, 249.toByte, 0.toByte),
  //       11 -> (71.toByte, 107.toByte, 160.toByte),
  //       12 -> (209.toByte, 221.toByte, 249.toByte),
  //       21 -> (221.toByte, 201.toByte, 201.toByte),
  //       22 -> (216.toByte, 147.toByte, 130.toByte),
  //       23 -> (237.toByte, 0.toByte, 0.toByte),
  //       24 -> (170.toByte, 0.toByte, 0.toByte),
  //       31 -> (178.toByte, 173.toByte, 163.toByte),
  //       32 -> (249.toByte, 249.toByte, 249.toByte),
  //       41 -> (104.toByte, 170.toByte, 99.toByte),
  //       42 -> (28.toByte, 99.toByte, 48.toByte),
  //       43 -> (181.toByte, 201.toByte, 142.toByte),
  //       51 -> (165.toByte, 140.toByte, 48.toByte),
  //       52 -> (204.toByte, 186.toByte, 124.toByte),
  //       71 -> (226.toByte, 226.toByte, 193.toByte),
  //       72 -> (201.toByte, 201.toByte, 119.toByte),
  //       73 -> (153.toByte, 193.toByte, 71.toByte),
  //       74 -> (119.toByte, 173.toByte, 147.toByte),
  //       81 -> (219.toByte, 216.toByte, 60.toByte),
  //       82 -> (170.toByte, 112.toByte, 40.toByte),
  //       90 -> (186.toByte, 216.toByte, 234.toByte),
  //       91 -> (181.toByte, 211.toByte, 229.toByte),
  //       92 -> (181.toByte, 211.toByte, 229.toByte),
  //       93 -> (181.toByte, 211.toByte, 229.toByte),
  //       94 -> (181.toByte, 211.toByte, 229.toByte),
  //       95 -> (112.toByte, 163.toByte, 186.toByte)
  //     )

  //     val commonValue: (Short, Short, Short) = (0, 0, 0)

  //     colorMap.size should equal (256)

  //     val dv = 255.0

  //     def convert(short: Short): Byte = math.floor(short / dv).toByte

  //     for (i <- 0 until colorMap.size) {
  //       val (v1, v2, v3) = colorMap(i)
  //       val c = (convert(v1), convert(v2), convert(v3))
  //       c should equal (nonCommonsMap.getOrElse(i, commonValue))
  //     }
  //   }

  }

  /*
   The proj4 string generator matches the listgeo -proj4 <file> command.

   The listgeo command sometimes drops precision compared to our generator,
   therefore we sometimes increase the epsilon double comparison value.
   */
  describe("reads GeoTiff CRS correctly") {

    it("should read slope.tif CS correctly") {
      val crs = SingleBandGeoTiff.compressed(s"$baseDataPath/slope.tif")crs

      val correctCRS = CRS.fromString("+proj=utm +zone=10 +datum=NAD27 +units=m +no_defs")

      crs should equal(correctCRS)
    }

    it("should read aspect.tif CS correctly") {
      val crs = SingleBandGeoTiff.compressed(s"$baseDataPath/aspect.tif").crs

      val correctProj4String = "+proj=lcc +lat_1=36.16666666666666 +lat_2=34.33333333333334 +lat_0=33.75 +lon_0=-79 +x_0=609601.22 +y_0=0 +datum=NAD83 +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(crs)
    }

    it("should read econic.tif CS correctly") {
      val crs = SingleBandGeoTiff.compressed(s"$baseDataPath/econic.tif").crs

      val correctProj4String = "+proj=eqdc +lat_0=33.76446202777777 +lon_0=-117.4745428888889 +lat_1=33.90363402777778 +lat_2=33.62529002777778 +x_0=0 +y_0=0 +datum=NAD27 +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it("should read bilevel.tif CS correctly") {
      val crs = SingleBandGeoTiff.compressed(geoTiffPath("bilevel.tif")).crs

      val correctProj4String = "+proj=tmerc +lat_0=0 +lon_0=-3.45233333 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it("should read all-ones.tif CS correctly") {
      val crs = SingleBandGeoTiff.compressed(geoTiffPath("all-ones.tif")).crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

    it("should read colormap.tif CS correctly") {
      val crs = SingleBandGeoTiff.compressed(geoTiffPath("colormap.tif")).crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")
      crs should equal(correctCRS)
    }

    it("should read us_ext_clip_esri.tif CS correctly") {
      val crs = SingleBandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri.tif")).crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

  }

  describe("reads file data correctly") {

    val MeanEpsilon = 1e-8

    def testMinMaxAndMean(min: Double, max: Double, mean: Double, file: String) {
      val SingleBandGeoTiff(tile, extent, _, _) = SingleBandGeoTiff.compressed(s"$baseDataPath/$file")

      tile.zonalMax(extent, extent.toPolygon) should be (max)
      tile.zonalMin(extent, extent.toPolygon) should be (min)
      tile.zonalMean(extent, extent.toPolygon) should be (mean +- MeanEpsilon)
    }

    it("should read UINT 16 little endian files correctly") {
      val min = 71
      val max = 237
      val mean = 210.66777801514
      val file = "reproject/nlcd_tile_wsg84.tif"

      testMinMaxAndMean(min, max, mean, file)
    }

    it("should read FLOAT 32 little endian files correctly") {
      val min = 0
      val max = 360
      val mean = 190.02287812187
      val file = "aspect.tif"

      testMinMaxAndMean(min, max, mean, file)
    }

    it("should read GeoTiff without GeoKey Directory correctly") {
      val SingleBandGeoTiff(tile, extent, crs, _) = SingleBandGeoTiff.compressed(geoTiffPath("no-geokey-dir.tif"))

      crs should be (LatLng)
      extent should be (Extent(307485, 3911490, 332505, 3936510))

      val (max, min, mean) = (74032, -20334, 17.023709809131)

      tile.zonalMax(extent, extent.toPolygon) should be (max)
      tile.zonalMin(extent, extent.toPolygon) should be (min)
      tile.zonalMean(extent, extent.toPolygon) should be (mean +- MeanEpsilon)
    }

    it("should read GeoTiff with tags") {
      val tags = SingleBandGeoTiff.compressed(geoTiffPath("tags.tif")).tags

      tags("TILE_COL") should be ("6")
      tags("units") should be ("kg m-2 s-1")
      tags("lon#axis") should be ("X")
      tags("_FillValue") should be ("1e+20")
      tags("NC_GLOBAL#driving_model_ensemble_member") should be("r1i1p1")
    }

    it("should read GeoTiff with no extent data correctly") {
      val Raster(tile, extent) = SingleBandGeoTiff.compressed(geoTiffPath("tags.tif")).raster

      extent should be (Extent(0, 0, tile.cols, tile.rows))
    }

    it("should read GeoTiff with multiple bands correctly") {
      val mbTile  = MultiBandGeoTiff(geoTiffPath("multi-tag.tif")).tile

      mbTile.bandCount should be (4)

      cfor(0)(_ < 4, _ + 1) { i =>
        val tile = mbTile.band(i)
        tile.cellType should be (TypeByte)
        tile.dimensions should be ((500, 500))
      }
    }

    it("should read GeoTiff with bands metadata correctly") {
      val geoTiff = MultiBandGeoTiff(geoTiffPath("multi-tag.tif"))

      val tags = geoTiff.tags

      tags("HEADTAG") should be ("1")
      tags("TAG_TYPE") should be ("HEAD")
      tags.size should be (2)

      val bandCount = geoTiff.tile.bandCount

      bandCount should be (4)

      cfor(0)(_ < 4, _ + 1) { i =>
        val correctMetadata = Map(
          "BANDTAG" -> (i + 1).toString,
          "TAG_TYPE" -> s"BAND${i + 1}"
        )

        tags(i) should be (correctMetadata)
      }
    }

    it("should read GeoTiff with ZLIB compression and needs exact segment sizes") {
      val geoTiff = SingleBandGeoTiff.compressed(geoTiffPath("nex-pr-tile.tif"))

      val tile = geoTiff.tile
      cfor(0)(_ < tile.rows, _ + 1) { row =>
        cfor(0)(_ < tile.cols, _ + 1) { col =>
          isNoData(tile.get(col, row)) should be (true)
        }
      }
    }

    it("should read clipped GeoTiff with byte NODATA value") {
      val geoTiff = SingleBandGeoTiff.compressed(geoTiffPath("nodata-tag-byte.tif")).tile
      val geoTiff2 = SingleBandGeoTiff.compressed(geoTiffPath("nodata-tag-float.tif")).tile
      assertEqual(geoTiff.toArrayTile.convert(TypeFloat), geoTiff2)
    }

  }
}

class PackBitsGeoTiffReaderSpec extends FunSpec
    with TestEngine
    with GeoTiffTestUtils {

  describe("Reading geotiffs with PACKBITS compression") {
    it("asdf") {
      val expected = SingleBandGeoTiff.compressed(geoTiffPath("uncompressed/tiled/bit.tif")).tile
      val actual = SingleBandGeoTiff.compressed(geoTiffPath("uncompressed/tiled/bit.tif")).tile.toArrayTile

      val ar = actual.getDouble(257, 1)
      val cm = expected.getDouble(257, 1)

      expected.foreach { (col, row, z) =>
        if(col == 257 && row == 1) { println(z) }
      }

      actual.foreach { (col, row, z) =>
        if(col == 257 && row == 1) { println(z) }
      }

      println(ar, cm)
      assertEqual(actual, expected)
      assertEqual(expected, actual)
    }
    // it("must read econic_packbits.tif and match uncompressed file") {
    //   val actual = SingleBandGeoTiff.compressed(geoTiffPath("econic_packbits.tif")).tile
    //   val expected = SingleBandGeoTiff.compressed(s"$baseDataPath/econic.tif").tile

    //   assertEqual(actual, expected)
    // }

    // // TODO: Reinsate this. Failing because of unsigned.
    // it("must read previously erroring packbits compression .tif and match uncompressed file") {
    //   val expected = SingleBandGeoTiff.compressed(geoTiffPath("packbits-error-uncompressed.tif")).tile
    //   val actual = SingleBandGeoTiff.compressed(geoTiffPath("packbits-error.tif")).tile

    //   assertEqual(actual, expected)
    // }
  }
}
