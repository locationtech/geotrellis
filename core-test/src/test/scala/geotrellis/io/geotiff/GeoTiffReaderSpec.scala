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
      }
    )
  }

  /*describe ("read geotiffs without runtime errors") {

    it ("reads econic.tif") {
      readAndSave("econic.tif")
    }

    it ("reads aspect.tif") {
      readAndSave("aspect.tif")
    }

    it ("reads slope.tif") {
      readAndSave("slope.tif")
    }

    it ("reads packbits.tif") {
      readAndSave("packbits.tif")
    }

  }*/

  private def compareGeoTiffImages(decomp: GeoTiff, uncomp: GeoTiff) {
    decomp.imageDirectories.size must equal  (uncomp.imageDirectories.size)

    decomp.imageDirectories zip uncomp.imageDirectories foreach {
      case (first, second) =>

      /*val d = first.imageBytes.take(200)
      val u = second.imageBytes.take(200)

      println(s"first 2 in decomp: ${d.take(20).toString}")
      println(s"first 2 in uncomp: ${u.take(20).toString}")

      var r = 0
      for (i <- 0 until d.size) {
        if (d(i) != u(i)) {
          println(s"diff at index $i => d($i) = ${d(i)} != u($i) = ${u(i)}")
          r += 1;
        } else {
          println(s"same at index $i => ${d(i)}")
        }
        if (r > 10) 1 / 0
      }*/


      first.imageBytes.size must equal (second.imageBytes.size)
      first.imageBytes must equal (second.imageBytes)
    }
  }

  describe ("reading compressed file should yield same image array as uncompressed file") {

    it ("should read aspect_jpeg.tif and match uncompressed file") {

    }

    it ("should read econic_lzw.tif and match uncompressed file") {
      val decomp = read("econic_lzw.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read econic_packbits.tif and match uncompressed file") {
      val decomp = read("econic_packbits.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read econic_zlib.tif and match uncompressed file") {
      val decomp = read("econic_zlib.tif")
      val uncomp = read("econic.tif")

      compareGeoTiffImages(decomp, uncomp)
    }

    it ("should read econic_CCITTRLE.tif and match uncompressed file") {

    }

    it ("should read econic_CCITTFAX3.tif and match uncompressed file") {

    }

    it ("should read econic_CCITTFAX4.tif and match uncompressed file") {

    }
  }
}
