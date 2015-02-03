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

import geotrellis.raster.io.geotiff.reader._

import geotrellis.raster._
import geotrellis.raster.io.arg._
import geotrellis.raster.io.geotiff.GeoTiffTestUtils
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

  val argPath = "/tmp/"
  val filePath = "raster-test/data"

  override def afterAll = purge

  def writeRasterToArg(imgDir: ImageDirectory, path: String, imageName: String): Unit = {
    val tile = imgDir.bands.head.tile
    val extent = imgDir.metaData.extent
    val cellType = imgDir.metaData.cellType
    new ArgWriter(cellType).write(path, tile, extent, imageName)
  }

  private def readAndSave(fileName: String) {
    val geoTiff = GeoTiffReader.read(s"$filePath/$fileName")

    val ifd = geoTiff.imageDirectory

    val currentFileName = math.abs(ifd.hashCode) + "-" + fileName.substring(0,
      fileName.length - 4)

    val corePath = argPath + currentFileName
    val pathArg = corePath + ".arg"
    val pathJson = corePath + ".json"
    writeRasterToArg(ifd, corePath, currentFileName)

    addToPurge(pathArg)
    addToPurge(pathJson)
  }

  describe("reading file and saving output") {

    it("must read aspect.tif and save") {
      readAndSave("aspect.tif")
    }

  }

  describe("reading an ESRI generated Float32 geotiff with 0 NoData value") {

    it("matches an arg produced from geotrellis.gdal reader of that tif") {
      val tile = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/us_ext_clip_esri.tif")
        .firstBand.tile

      val expectedTile = ArgReader.read(s"$filePath/geotiff-reader-tiffs/us_ext_clip_esri.json")

      assertEqual(tile, expectedTile)
    }

  }

  describe("reading slope.tif") {

    it("should match the ARG version") {
      val path = "slope.tif"
      val argPath = s"$filePath/data/slope.json"

      val tile = GeoTiffReader.read(s"$filePath/$path").firstBand.tile

      val expectedTile = ArgReader.read(argPath)

      assertEqual(tile, expectedTile)
    }

  }

  // Apparently GDAL supports a ton of different compressions.
  // In the coming days we will work to add support for as many as possible.
  describe("reading compressed file must yield same image array as uncompressed file") {

    ignore ("must read aspect_jpeg.tif and match uncompressed file") {
      ??? // Need to implement JPEG decompression
    }

    it("must read econic_lzw.tif and match uncompressed file") {
      val decomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/econic_lzw.tif")
      val uncomp = GeoTiffReader.read(s"$filePath/econic.tif")

      decomp.firstBand.tile should equal(uncomp.firstBand.tile)
    }

    it("must read econic_packbits.tif and match uncompressed file") {
      val decomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/econic_packbits.tif")
      val uncomp = GeoTiffReader.read(s"$filePath/econic.tif")

      decomp.firstBand.tile should equal(uncomp.firstBand.tile)
    }

    it("must read econic_zlib.tif and match uncompressed file") {
      val decomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/econic_zlib.tif")
      val uncomp = GeoTiffReader.read(s"$filePath/econic.tif")

      decomp.firstBand.tile should equal(uncomp.firstBand.tile)
    }

    it("must read bilevel_CCITTRLE.tif and match uncompressed file") {
      val decomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel_CCITTRLE.tif")
      val uncomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel.tif")

      decomp.firstBand.tile should equal(uncomp.firstBand.tile)
    }

    it(s"$filePath/must GeoTiffReader.read bilevel_CCITTFAX3.tif and match uncompressed file") {
      val decomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel_CCITTFAX3.tif")
      val uncomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel.tif")

      decomp.firstBand.tile should equal(uncomp.firstBand.tile)
    }

    it(s"$filePath/must GeoTiffReader.read bilevel_CCITTFAX4.tif and match uncompressed file") {
      val decomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel_CCITTFAX4.tif")
      val uncomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel.tif")

      decomp.firstBand.tile should equal(uncomp.firstBand.tile)
    }

    it("must read all-ones.tif and match uncompressed file") {
      val decomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/all-ones.tif")
      val uncomp = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/all-ones-no-comp.tif")

      decomp.firstBand.tile should equal(uncomp.firstBand.tile)
    }
  }

  describe("reading tiled file must yield same image as strip files") {

    it("must read bilevel_tiled.tif and match strip file") {
      val tiled = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel_tiled.tif")
      val striped = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/bilevel.tif")

      tiled.firstBand.tile should equal(striped.firstBand.tile)
    }

    it("must read us_ext_clip_esri.tif and match strip file") {
      val tiled = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/us_ext_clip_esri.tif")
      val striped = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/us_ext_clip_esri_stripes.tif")

      tiled.firstBand.tile should equal(striped.firstBand.tile)
    }

  }

  describe("match tiff tags and geokeys correctly") {

    it("must match aspect.tif tiff tags") {
      val ifd = GeoTiffReader.read(s"$filePath/aspect.tif").imageDirectory

      ifd.cols should equal (1500L)

      ifd.rows should equal (1350L)

      (ifd &|-> ImageDirectory._basicTags ^|->
        BasicTags._bitsPerSample get) match {
        case Some(v) if (v.size == 1) => v(0) should equal (32)
        case None => fail
      }

      ifd.compression should equal (1)

      (ifd &|-> ImageDirectory._basicTags ^|->
        BasicTags._photometricInterp get) should equal (1)

      (ifd &|-> ImageDirectory._basicTags ^|->
        BasicTags._stripOffsets get) match {
        case Some(stripOffsets) => stripOffsets.size should equal (1350)
        case None => fail
      }

      (ifd &|-> ImageDirectory._basicTags ^|->
        BasicTags._samplesPerPixel get) should equal (1)

      (ifd &|-> ImageDirectory._basicTags ^|->
        BasicTags._rowsPerStrip get) should equal (1L)

      (ifd &|-> ImageDirectory._basicTags ^|->
        BasicTags._stripByteCounts get) match {
        case Some(stripByteCounts) => stripByteCounts.size should equal (1350)
        case None => fail
      }

      (ifd &|-> ImageDirectory._nonBasicTags ^|->
        NonBasicTags._planarConfiguration get) match {
        case Some(planarConfiguration) => planarConfiguration should equal (1)
        case None => fail
      }

      val sampleFormats = (ifd &|-> ImageDirectory._dataSampleFormatTags
        ^|-> DataSampleFormatTags._sampleFormat get)
      sampleFormats.size should equal (1)
      sampleFormats(0) should equal (3)

      (ifd &|-> ImageDirectory._geoTiffTags
        ^|-> GeoTiffTags._modelPixelScale get) match {
        case Some(modelPixelScales) => {
          modelPixelScales._1 should equal (10.0)
          modelPixelScales._2 should equal (10.0)
          modelPixelScales._3 should equal (0.0)
        }
        case None => fail
      }

      (ifd &|-> ImageDirectory._geoTiffTags
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

      (ifd &|-> ImageDirectory._geoTiffTags
        ^|-> GeoTiffTags._gdalInternalNoData get) match {
        case Some(gdalInternalNoData) => gdalInternalNoData should equal (-9999.0)
        case None => fail
      }
    }

    it("must match aspect.tif geokeys") {
      val ifd = GeoTiffReader.read(s"$filePath/aspect.tif").imageDirectory

      ifd.hasPixelArea should be (true)

      val metaData = ifd.metaData

      val minX = metaData.extent.xmin should equal (630000.0)
      val minY = metaData.extent.ymin should equal (215000.0)
      val maxX = metaData.extent.xmax should equal (645000.0)
      val maxY = metaData.extent.ymax should equal (228500.0)

      metaData.cellType should equal (TypeFloat)

      val knownNoData = -9999f

      val image = ifd.imageBytes

      var i = 0
      val bb = ByteBuffer.allocate(4)
      while (i < image.size) {
        for (j <- i until i + 4) bb.put(image(i))

        bb.position(0)
        val f = bb.getFloat
        if (f == knownNoData) fail
        bb.position(0)

        i += 4
      }

    }

    it("must match colormap.tif colormap") {
      val ifd = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/colormap.tif")
        .imageDirectory

      val colorMap = (ifd &|->
        ImageDirectory._basicTags ^|->
        BasicTags._colorMap get)

      val nonCommonsMap = collection.immutable.HashMap[Int, (Byte, Byte, Byte)](
        1 -> (0.toByte, 249.toByte, 0.toByte),
        11 -> (71.toByte, 107.toByte, 160.toByte),
        12 -> (209.toByte, 221.toByte, 249.toByte),
        21 -> (221.toByte, 201.toByte, 201.toByte),
        22 -> (216.toByte, 147.toByte, 130.toByte),
        23 -> (237.toByte, 0.toByte, 0.toByte),
        24 -> (170.toByte, 0.toByte, 0.toByte),
        31 -> (178.toByte, 173.toByte, 163.toByte),
        32 -> (249.toByte, 249.toByte, 249.toByte),
        41 -> (104.toByte, 170.toByte, 99.toByte),
        42 -> (28.toByte, 99.toByte, 48.toByte),
        43 -> (181.toByte, 201.toByte, 142.toByte),
        51 -> (165.toByte, 140.toByte, 48.toByte),
        52 -> (204.toByte, 186.toByte, 124.toByte),
        71 -> (226.toByte, 226.toByte, 193.toByte),
        72 -> (201.toByte, 201.toByte, 119.toByte),
        73 -> (153.toByte, 193.toByte, 71.toByte),
        74 -> (119.toByte, 173.toByte, 147.toByte),
        81 -> (219.toByte, 216.toByte, 60.toByte),
        82 -> (170.toByte, 112.toByte, 40.toByte),
        90 -> (186.toByte, 216.toByte, 234.toByte),
        91 -> (181.toByte, 211.toByte, 229.toByte),
        92 -> (181.toByte, 211.toByte, 229.toByte),
        93 -> (181.toByte, 211.toByte, 229.toByte),
        94 -> (181.toByte, 211.toByte, 229.toByte),
        95 -> (112.toByte, 163.toByte, 186.toByte)
      )

      val commonValue: (Short, Short, Short) = (0, 0, 0)

      colorMap.size should equal (256)

      val dv = 255.0

      def convert(short: Short): Byte = math.floor(short / dv).toByte

      for (i <- 0 until colorMap.size) {
        val (v1, v2, v3) = colorMap(i)
        val c = (convert(v1), convert(v2), convert(v3))
        c should equal (nonCommonsMap.getOrElse(i, commonValue))
      }
    }

  }

  /*
   The proj4 string generator matches the listgeo -proj4 <file> command.

   The listgeo command sometimes drops precision compared to our generator,
   therefore we sometimes increase the epsilon double comparison value.
   */
  describe("reads GeoTiff CS correctly") {

    it("should read slope.tif CS correctly") {
      val crs = GeoTiffReader.read(s"$filePath/slope.tif").firstBand.crs

      val correctCRS = CRS.fromString("+proj=utm +zone=10 +datum=NAD27 +units=m +no_defs")

      crs should equal(correctCRS)
    }

    it("should read aspect.tif CS correctly") {
      val crs = GeoTiffReader.read(s"$filePath/aspect.tif").firstBand.crs

      val correctProj4String = "+proj=lcc +lat_1=36.16666666666666 +lat_2=34.33333333333334 +lat_0=33.75 +lon_0=-79 +x_0=609601.22 +y_0=0 +datum=NAD83 +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(crs)
    }

    it("should read econic.tif CS correctly") {
      val crs = GeoTiffReader.read(s"$filePath/econic.tif").firstBand.crs

      val correctProj4String = "+proj=eqdc +lat_0=33.76446202777777 +lon_0=-117.4745428888889 +lat_1=33.90363402777778 +lat_2=33.62529002777778 +x_0=0 +y_0=0 +datum=NAD27 +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it("should read bilevel.tif CS correctly") {
      val crs = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/bilevel.tif")
        .firstBand
        .crs

      val correctProj4String = "+proj=tmerc +lat_0=0 +lon_0=-3.45233333 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it("should read all-ones.tif CS correctly") {
      val crs = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/all-ones.tif")
        .firstBand
        .crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

    it("should read colormap.tif CS correctly") {
      val crs = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/colormap.tif")
        .firstBand
        .crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

    it("should read us_ext_clip_esri.tif CS correctly") {
      val crs = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/us_ext_clip_esri.tif")
        .firstBand
        .crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

  }

  describe("reads file data correctly") {

    val MeanEpsilon = 1e-8

    def testMinMaxAndMean(min: Double, max: Double, mean: Double, file: String) {
      val GeoTiffBand(tile, extent, _, _) = GeoTiffReader
        .read(s"$filePath/$file")
        .firstBand

      tile.zonalMax(extent, extent.toPolygon) should be (max)
      tile.zonalMin(extent, extent.toPolygon) should be (min)
      tile.zonalMean(extent, extent.toPolygon) should be (mean +- MeanEpsilon)
    }

    it("should read UINT 16 little endian files correctly") {
      val min = 71
      val max = 237
      val mean = 210.66777801514
      val file = "/reproject/nlcd_tile_wsg84.tif"

      testMinMaxAndMean(min, max, mean, file)
    }

    it("should read FLOAT 32 little endian files correctly") {
      val min = 0
      val max = 360
      val mean = 190.02287812187
      val file = "/aspect.tif"

      testMinMaxAndMean(min, max, mean, file)
    }

    it("should read GeoTiff without GeoKey Directory correctly") {
      val GeoTiffBand(tile, extent, crs, _) = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/no-geokey-dir.tif")
        .firstBand

      crs should be (LatLng)
      extent should be (Extent(307485, 3911490, 332505, 3936510))

      val (max, min, mean) = (74032, -20334, 17.023709809131)

      tile.zonalMax(extent, extent.toPolygon) should be (max)
      tile.zonalMin(extent, extent.toPolygon) should be (min)
      tile.zonalMean(extent, extent.toPolygon) should be (mean +- MeanEpsilon)
    }

    it("should read GeoTiff with GDAL Metadata correctly") {
      val metadata = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/gdal-metadata.tif")
        .tags

      metadata("TILE_COL") should be ("6")
      metadata("units") should be ("kg m-2 s-1")
      metadata("lon#axis") should be ("X")
      metadata("_FillValue") should be ("1e+20")
      metadata("NC_GLOBAL#driving_model_ensemble_member") should be("r1i1p1")
    }

    it("should read GeoTiff with no extent data correctly") {
      val GeoTiffBand(tile, extent, _, _) = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/gdal-metadata.tif")
        .firstBand

      extent should be (Extent(0, 0, tile.cols, tile.rows))
    }

    it("should read GeoTiff with multiple bands correctly") {
      val bands = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/multi-tag.tif")
        .bands

      bands.size should be (4)

      cfor(0)(_ < 4, _ + 1) { i =>
        val band = bands(i)
        band.tile.cellType should be (TypeByte)
        band.tile.dimensions should be ((500, 500))
      }
    }

    it("should read GeoTiff with bands metadata correctly") {
      val geoTiff = GeoTiffReader
        .read(s"$filePath/geotiff-reader-tiffs/multi-tag.tif")

      val tags = geoTiff.tags

      tags("HEADTAG") should be ("1")
      tags("TAG_TYPE") should be ("HEAD")
      tags.size should be (2)

      val bands = geoTiff.bands

      bands.size should be (4)

      cfor(0)(_ < 4, _ + 1) { i =>
        val correctMetadata = Map(
          "BANDTAG" -> (i + 1).toString,
          "TAG_TYPE" -> s"BAND${i + 1}"
        )

        bands(i).tags should be (correctMetadata)
      }
    }

    it("should read GeoTiff with ZLIB compression and needs exact segment sizes") {
      val geoTiff = GeoTiffReader.read(s"$filePath/geotiff-reader-tiffs/nex-pr-tile.tif")

      val tile = geoTiff.firstBand.tile
      cfor(0)(_ < tile.cols, _ + 1) { i =>
        cfor(0)(_ < tile.rows, _ + 1) { j =>
          isNoData(tile.get(i, j)) should be (true)
        }
      }
    }
  }
}
