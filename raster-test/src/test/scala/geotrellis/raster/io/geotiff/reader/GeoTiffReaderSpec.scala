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

import monocle.syntax._

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.ImageDirectoryLenses._
import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster.io.geotiff.GeoTiffTestUtils
import geotrellis.testkit._
import geotrellis.proj4.CRS

import scala.io.{Source, Codec}

import scala.collection.immutable.HashMap

import java.util.BitSet
import java.nio.ByteBuffer

import org.scalatest._

class GeoTiffReaderSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with TestEngine
    with GeoTiffTestUtils {

  val argPath = "/tmp/"
  val filePathToTestData = "raster-test/data/"

  override def afterAll = purge

  private def read(fileName: String): GeoTiff = {
    val filePath = filePathToTestData + fileName
    GeoTiffReader(filePath).read
  }

  private def readAndSave(fileName: String) {
    val geoTiff = read(fileName)

    geoTiff.imageDirectories.foreach(ifd => {
      val currentFileName = math.abs(ifd.hashCode) + "-" + fileName.substring(0,
        fileName.length - 4)

      val corePath = argPath + currentFileName
      val pathArg = corePath + ".arg"
      val pathJson = corePath + ".json"
      ifd.writeRasterToArg(corePath, currentFileName)

      addToPurge(pathArg)
      addToPurge(pathJson)
    })
  }

  private def compareGeoTiffImages(first: GeoTiff, second: GeoTiff) {
    first.imageDirectories.size should equal (second.imageDirectories.size)

    first.imageDirectories zip second.imageDirectories foreach {
      case (firstIFD, secondIFD) =>

        firstIFD.imageBytes.size should equal (secondIFD.imageBytes.size)

        firstIFD.imageBytes should equal (secondIFD.imageBytes)
    }
  }

  describe ("reading file and saving output") {

    it ("must read aspect.tif and save") {
      readAndSave("aspect.tif")
    }

  }

  describe ("reading an ESRI generated Float32 geotiff with 0 NoData value") {

    it("matches an arg produced from geotrellis.gdal reader of that tif") {
      val (readTile, _, _) =
        read("geotiff-reader-tiffs/us_ext_clip_esri.tif")
          .imageDirectories.head.toRaster

      val expectedTile =
        ArgReader.read(s"$filePathToTestData/geotiff-reader-tiffs/us_ext_clip_esri.json")

      assertEqual(readTile, expectedTile)
    }

  }

  describe ("reading slope.tif") {
    it("should match the ARG version") {
      val path = "slope.tif"
      val argPath = s"$filePathToTestData/data/slope.json"

      val (readTile, _, _) =
        read(path)
          .imageDirectories.head.toRaster

      val expectedTile =
        ArgReader.read(argPath)

      assertEqual(readTile, expectedTile)
    }
  }

  // Apparently GDAL supports a ton of different compressions.
  // In the coming days we will work to add support for as many as possible.
  describe ("reading compressed file must yield same image array as uncompressed file") {

    ignore ("must read aspect_jpeg.tif and match uncompressed file") {

    }

    it ("must read econic_lzw.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/econic_lzw.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("must read econic_packbits.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/econic_packbits.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("must read econic_zlib.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/econic_zlib.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("must read bilevel_CCITTRLE.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/bilevel_CCITTRLE.tif")
      val uncomp = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("must read bilevel_CCITTFAX3.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/bilevel_CCITTFAX3.tif")
      val uncomp = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("must read bilevel_CCITTFAX4.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/bilevel_CCITTFAX4.tif")
      val uncomp = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("must read all-ones.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/all-ones.tif")
      val uncomp = read("geotiff-reader-tiffs/all-ones-no-comp.tif")

      compareGeoTiffImages(decomp, uncomp)
    }
  }

  describe ("reading tiled file must yield same image as strip files") {

    it ("must read bilevel_tiled.tif and match strip file") {
      val tiled = read("geotiff-reader-tiffs/bilevel_tiled.tif")
      val striped = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(tiled, striped)
    }

    it ("must read us_ext_clip_esri.tif and match strip file") {
      val tiled = read("geotiff-reader-tiffs/us_ext_clip_esri.tif")
      val striped = read("geotiff-reader-tiffs/us_ext_clip_esri_stripes.tif")

      compareGeoTiffImages(tiled, striped)
    }

  }

  describe ("match tiff tags and geokeys correctly") {

    it ("must match aspect.tif tiff tags") {
      val aspect = read("aspect.tif")

      val ifd = aspect.imageDirectories(0)

      (ifd |-> imageWidthLens get) should equal (1500L)

      (ifd |-> imageLengthLens get) should equal (1350L)

      ifd |-> bitsPerSampleLens get match {
        case Some(v) if (v.size == 1) => v(0) should equal (32)
        case None => fail
      }

      (ifd |-> compressionLens get) should equal (1)

      (ifd |-> photometricInterpLens get) should equal (1)

      ifd |-> stripOffsetsLens get match {
        case Some(stripOffsets) => stripOffsets.size should equal (1350)
        case None => fail
      }

      (ifd |-> samplesPerPixelLens get) should equal (1)

      (ifd |-> rowsPerStripLens get) should equal (1L)

      ifd |-> stripByteCountsLens get match {
        case Some(stripByteCounts) => stripByteCounts.size should equal (1350)
        case None => fail
      }

      ifd |-> planarConfigurationLens get match {
        case Some(planarConfiguration) => planarConfiguration should equal (1)
        case None => fail
      }

      val sampleFormats = (ifd |-> sampleFormatLens get)
      sampleFormats.size should equal (1)
      sampleFormats(0) should equal (3)

      ifd |-> modelPixelScaleLens get match {
        case Some(modelPixelScales) => {
          modelPixelScales._1 should equal (10.0)
          modelPixelScales._2 should equal (10.0)
          modelPixelScales._3 should equal (0.0)
        }
        case None => fail
      }

      ifd |-> modelTiePointsLens get match {
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

      ifd |-> gdalInternalNoDataLens get match {
        case Some(gdalInternalNoData) => gdalInternalNoData should equal (-9999.0)
        case None => fail
      }
    }

    it ("must match aspect.tif geokeys") {
      val aspect = read("aspect.tif")

      val ifd = aspect.imageDirectories(0)

      ifd.hasPixelArea should be (true)

      val minX = ifd.extent.xmin should equal (630000.0)
      val minY = ifd.extent.ymin should equal (215000.0)
      val maxX = ifd.extent.xmax should equal (645000.0)
      val maxY = ifd.extent.ymax should equal (228500.0)

      ifd.cellType should equal (TypeFloat)

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

    it ("must match colormap.tif colormap") {
      val colorMappedTiff = read("geotiff-reader-tiffs/colormap.tif")

      val ifd = colorMappedTiff.imageDirectories(0)

      val colorMap = (ifd |-> colorMapLens get) getOrElse fail

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
  describe ("reads GeoTiff CS correctly") {

    it ("should read slope.tif CS correctly") {
      val tiff = read("slope.tif")

      val crs = tiff.imageDirectories.head.crs

      val correctCRS = CRS.fromString("+proj=utm +zone=10 +datum=NAD27 +units=m +no_defs")

      crs should equal(correctCRS)
    }

    it ("should read aspect.tif CS correctly") {
      val tiff = read("aspect.tif")

      val crs = tiff.imageDirectories.head.crs

      val correctProj4String =
        "+proj=lcc +lat_1=36.16666666666666 +lat_2=34.33333333333334 +lat_0=33.75 +lon_0=-79 +x_0=609601.22 +y_0=0 +datum=NAD83 +units=m +no_defs"
      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(crs)
    }

    it ("should read econic.tif CS correctly") {
      val tiff = read("econic.tif")

      val crs = tiff.imageDirectories.head.crs

      val correctProj4String =
        "+proj=eqdc +lat_0=33.76446202777777 +lon_0=-117.4745428888889 +lat_1=33.90363402777778 +lat_2=33.62529002777778 +x_0=0 +y_0=0 +datum=NAD27 +units=m +no_defs"
      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it ("should read bilevel.tif CS correctly") {
      val tiff = read("geotiff-reader-tiffs/bilevel.tif")

      println("LOOK")
      val crs = tiff.imageDirectories.head.crs

      println(crs.toProj4String)

      val correctProj4String = "+proj=tmerc +lat_0=0 +lon_0=-3.45233333 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it ("should read all-ones.tif CS correctly") {
      val tiff = read("geotiff-reader-tiffs/all-ones.tif")

      val crs = tiff.imageDirectories.head.crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

    it ("should read colormap.tif CS correctly") {
      val tiff = read("geotiff-reader-tiffs/colormap.tif")

      val crs = tiff.imageDirectories.head.crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

    it ("should read us_ext_clip_esri.tif CS correctly") {
      val tiff = read("geotiff-reader-tiffs/us_ext_clip_esri.tif")

      val crs = tiff.imageDirectories.head.crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

  }

}
