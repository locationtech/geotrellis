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
import monocle.Macro._

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.ImageDirectoryLenses._
import geotrellis.testkit._

import scala.io.{Source, Codec}

import java.io.File
import java.util.BitSet
import java.nio.ByteBuffer

import org.scalatest._

class GeoTiffReaderSpec extends FunSpec with MustMatchers with BeforeAndAfterAll {

  var writtenFiles = Vector[String]()

  def addToWrittenFiles(path: String) = synchronized {
    writtenFiles = writtenFiles :+ path
  }

  override def afterAll() = writtenFiles foreach { path =>
    val file = new File(path)
    if (file.exists()) file.delete()
  }

  val argPath = "/tmp/"
  val filePathToTestData = "raster-test/data/"


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

      val corePath = argPath + currentFileName
      val pathArg = corePath + ".arg"
      val pathJson = corePath + ".json"
      ifd.writeRasterToArg(corePath, currentFileName)

      addToWrittenFiles(pathArg)
      addToWrittenFiles(pathJson)
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

  describe ("reading file and saving output") {

    it ("must read aspect.tif and save") {
      readAndSave("aspect.tif")
    }

  }

  describe ("reading compressed file must yield same image array as uncompressed file") {

    // This is the last bit left of the geotiff reader before it becomes
    // fully compliant with all the compression format GDAL supports.
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
  }

  describe ("reading tiled file must yield same image as strip files") {

    it ("must read bilevel_tiled.tif and match strip file") {
      val tiled = read("geotiff-reader-tiffs/bilevel_tiled.tif")
      val striped = read("geotiff-reader-tiffs/bilevel.tif")

      compareGeoTiffImages(tiled, striped)
    }

  }

  describe ("match tiff tags and geokeys correctly") {

    it ("must match aspect.tif tiff tags") {
      val aspect = read("aspect.tif")

      val ifd = aspect.imageDirectories(0)

      (ifd |-> imageWidthLens get) must equal (1500L)

      (ifd |-> imageLengthLens get) must equal (1350L)

      ifd |-> bitsPerSampleLens get match {
        case Some(v) if (v.size == 1) => v(0) must equal (32)
        case None => fail
      }

      (ifd |-> compressionLens get) must equal (1)

      ifd |-> photometricInterpLens get match {
        case Some(pi) => pi must equal (1)
        case None => fail
      }

      ifd |-> stripOffsetsLens get match {
        case Some(stripOffsets) => stripOffsets.size must equal (1350)
        case None => fail
      }

      (ifd |-> samplesPerPixelLens get) must equal (1)

      (ifd |-> rowsPerStripLens get) must equal (1L)

      ifd |-> stripByteCountsLens get match {
        case Some(stripByteCounts) => stripByteCounts.size must equal (1350)
        case None => fail
      }

      ifd |-> planarConfigurationLens get match {
        case Some(planarConfiguration) => planarConfiguration must equal (1)
        case None => fail
      }

      val sampleFormats = (ifd |-> sampleFormatLens get)
      sampleFormats.size must equal (1)
      sampleFormats(0) must equal (3)

      ifd |-> modelPixelScaleLens get match {
        case Some(modelPixelScales) => {
          modelPixelScales._1 must equal (10.0)
          modelPixelScales._2 must equal (10.0)
          modelPixelScales._3 must equal (0.0)
        }
        case None => fail
      }

      ifd |-> modelTiePointsLens get match {
        case Some(modelTiePoints) if (modelTiePoints.size == 1) => {
          val (p1, p2) = modelTiePoints(0)
          p1.x must equal (0.0)
          p1.y must equal (0.0)
          p1.z must equal (0.0)
          p2.x must equal (630000.0)
          p2.y must equal (228500.0)
          p2.z must equal (0.0)
        }
        case None => fail
      }

      ifd |-> gdalInternalNoDataLens get match {
        case Some(gdalInternalNoData) => gdalInternalNoData must equal ("-9999")
        case None => fail
      }
    }

    it ("must match aspect.tif geokeys") {
      val aspect = read("aspect.tif")

      val ifd = aspect.imageDirectories(0)

      ifd.hasPixelArea must be (true)

      val minX = ifd.extent.xmin must equal (630000.0)
      val minY = ifd.extent.ymin must equal (215000.0)
      val maxX = ifd.extent.xmax must equal (645000.0)
      val maxY = ifd.extent.ymax must equal (228500.0)

      ifd.cellType must equal (TypeFloat)

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
  }

}
