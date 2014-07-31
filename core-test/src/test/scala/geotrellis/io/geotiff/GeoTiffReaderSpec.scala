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

package geotrellis.io.geotiff

import geotrellis.raster._
import geotrellis.source._
import geotrellis.process._
import geotrellis.testkit._

import scala.io.{Source, Codec}

import java.util.BitSet

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

class GeoTiffReaderSpec extends FunSpec
    with MustMatchers
    with RasterBuilders
    with TestServer {

  val argPath = "core-test/data/data/"
  val filePathToTestData = "core-test/data/"

  private def read(fileName: String): GeoTiff = {
    val filePath = filePathToTestData + fileName
    val source = Source.fromFile(filePath)(Codec.ISO8859)

    val geotiff = GeoTiffReader(source).read

    source.close

    geotiff
  }

  private def readAndSave(fileName: String) {
    val geoTiff = read(fileName)

    geoTiff.imageDirectories.foreach(ifd => {
      val currentFileName = math.abs(ifd.hashCode) + "-" + fileName.substring(0,
        fileName.length - 4)

      ifd.writeRasterToArg(argPath + currentFileName, currentFileName)
    })
  }

  private def compareGeoTiffImages(first: GeoTiff, second: GeoTiff) {
    first.imageDirectories.size must equal (second.imageDirectories.size)

    first.imageDirectories zip second.imageDirectories foreach {
      case (firstIFD, secondIFD) =>

        firstIFD.imageBytes.size must equal (secondIFD.imageBytes.size)
        firstIFD.imageBytes must equal (secondIFD.imageBytes)
    }
  }

  /*describe ("reading file and saving output") {

    it ("should read aspect.tif and save") {
      readAndSave("aspect.tif")
    }

  }*/

  describe ("reading compressed file should yield same image array as uncompressed file") {

    it ("should read aspect_jpeg.tif and match uncompressed file") {

    }

    it ("should read econic_lzw.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/econic_lzw.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read econic_packbits.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/econic_packbits.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read econic_zlib.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/econic_zlib.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read bilevel_CCITTRLE.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/bilevel_CCITTRLE.tif")
      val uncomp = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read bilevel_CCITTFAX3.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/bilevel_CCITTFAX3.tif")
      val uncomp = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read bilevel_CCITTFAX4.tif and match uncompressed file") {
      val decomp = read("geotiff-reader-tiffs/bilevel_CCITTFAX4.tif")
      val uncomp = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(decomp, uncomp)
    }
  }

  describe ("reading tiled file should yield same image as strip files") {

    it ("should read bilevel_tiled.tif and match strip file") {
      val tiled = read("geotiff-reader-tiffs/bilevel_tiled.tif")
      val striped = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(tiled, striped)
    }

  }
}
