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

package geotrellis.geotools

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.vector._
import geotrellis.vector.testkit._

import javax.media.jai.iterator.RectIterFactory
import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.gce.geotiff._
import org.opengis.parameter.GeneralParameterValue
import org.scalatest._
import spire.syntax.cfor._

class GridCoverage2DConvertersSpec extends FunSpec with Matchers with GeoTiffTestUtils {

  case class TestFile(description: String, path: String, isMultiband: Boolean) {
    def gridCoverage2D: GridCoverage2D =
      new GeoTiffReader(new java.io.File(path)).read(null)

    def singlebandRaster: ProjectedRaster[Tile] = {
      val tiff = SinglebandGeoTiff(path)
      tiff.projectedRaster.copy(raster = Raster(tiff.tile.toArrayTile, tiff.extent))
    }

    def multibandRaster: ProjectedRaster[MultibandTile] = {
      val tiff = MultibandGeoTiff(path)
      tiff.projectedRaster.copy(raster = Raster(tiff.tile.toArrayTile, tiff.extent))
    }
  }

  val testFilesWithProjections: List[TestFile] =
    List(
      TestFile("Bit raster", geoTiffPath("deflate/striped/bit.tif"), false),
      TestFile("Short raster", geoTiffPath("deflate/striped/int16.tif"), false),
      TestFile("UShort raster", geoTiffPath("deflate/striped/uint16.tif"), false),
      TestFile("Int raster", geoTiffPath("deflate/striped/int32.tif"), false),
      TestFile("Int 3 band", geoTiffPath("3bands/3bands-tiled-pixel.tif"), true),
      TestFile("Float raster", geoTiffPath("deflate/striped/float32.tif"), false),
      TestFile("Float 3 band pixel interleave", geoTiffPath("3bands/float32/3bands-striped-pixel.tif"), true),
      TestFile("Float 3 band band interleave", geoTiffPath("3bands/float32/3bands-tiled-band.tif"), true),
      TestFile("Double raster", geoTiffPath("deflate/striped/float64.tif"), false),
      TestFile("2 band uint16", geoTiffPath("r-nir.tif"), true)
    )

  val testFilesWithoutProjections: List[TestFile] =
    List(
      TestFile("UByte raster", geoTiffPath("1band/aspect_byte_uncompressed_tiled.tif"), false)
    )


  val testFilesGTandBackWithProjections: List[TestFile] =
    List(
      // GeoTools GeoTiff reader doesn't seem to be handling unsigned types correctly.
      // TODO: Fix our understanding of it, or fix GeoTools.
      TestFile("UInt raster", geoTiffPath("deflate/striped/uint32.tif"), false)
    )

  val testFilesGTandBackWithoutProjections: List[TestFile] =
    List(
      // There seems to be a bug in the way that GeoTools handles signed byte rasters.
      // See: https://sourceforge.net/p/geotools/mailman/message/35128131/
      // TODO: Uncomment when it's clear how to make it pass.
      // TestFile("Byte contstant nd raster", s"$baseDataPath/sbn/SBN_inc_percap-nodata-clip.tif", false)
    )


  def assertEqual(actual: ProjectedRaster[Tile], expected: ProjectedRaster[Tile]): Unit = {
    actual.crs should be (expected.crs)
    assertEqual(actual.raster, expected.raster)
  }

  def assertEqual(actual: Raster[Tile], expected: Raster[Tile]): Unit = {
    val Raster(actualTile, actualExtent) = actual
    val Raster(expectedTile, expectedExtent) = expected

    actualExtent should matchGeom (expectedExtent, 0.00000001)

    actualTile.cellType should be (expectedTile.cellType)

    actualTile.cols should be (expectedTile.cols)
    actualTile.rows should be (expectedTile.rows)

    expectedTile.foreachDouble { (col, row, z) =>
      val z2 = actualTile.getDouble(col, row)
      withClue(s"ACTUAL: ${z2}  EXPECTED: $z  COL $col ROW $row") {
        if(isNoData(z)) { isNoData(z2) should be (true) }
        else { z2 should be (z) }
      }
    }
  }

  def assertEqual(actual: ProjectedRaster[MultibandTile], expected: ProjectedRaster[MultibandTile])(implicit d: DummyImplicit): Unit = {
    actual.crs should be (expected.crs)
    assertEqual(actual.raster, expected.raster)
  }

  def assertEqual(actual: Raster[MultibandTile], expected: Raster[MultibandTile])(implicit d: DummyImplicit): Unit = {
    val Raster(actualTile, actualExtent) = actual
    val Raster(expectedTile, expectedExtent) = expected

    actualExtent should matchGeom (expectedExtent, 0.00000001)

    actualTile.cellType should be (expectedTile.cellType)

    actualTile.cols should be (expectedTile.cols)
    actualTile.rows should be (expectedTile.rows)

    actualTile.bandCount should be (expectedTile.bandCount)

    for(b <- 0 until actualTile.bandCount) {
      val actualBand = actualTile.band(b)
      val expectedBand = expectedTile.band(b)

      expectedBand.foreachDouble { (col, row, z) =>
        val z2 = actualBand.getDouble(col, row)
        withClue(s"ACTUAL: $z2  EXPECTED: $z  COL $col ROW $row") {
          if(isNoData(z)) { isNoData(z2) should be (true) }
          else { z2 should be (z) }
        }
      }
    }
  }

  def assertEqual(actual: GridCoverage2D, expected: GridCoverage2D): Unit = {
    def getInfo(coverage: GridCoverage2D): (Int, Int, Int) = {
      val dimensions = coverage.getGridGeometry.getGridRange
      val maxDimensions = dimensions.getHigh()
      val cols = maxDimensions.getCoordinateValue(0) + 1
      val rows = maxDimensions.getCoordinateValue(1) + 1
      val bandCount = actual.getNumSampleDimensions
      (cols, rows, bandCount)
    }

    val (actualCols, actualRows, actualBandCount) = getInfo(actual)
    val (expectedCols, expectedRows, expectedBandCount) = getInfo(expected)

    actualCols should be (expectedCols)
    actualRows should be (expectedRows)
    actualBandCount should be (expectedBandCount)

    // This way is too slow
    // val actualValues = Array.ofDim[Double](actualBandCount)
    // val expectedValues = Array.ofDim[Double](expectedBandCount)
    // cfor(0)(_ < actualCols, _ + 1) { col =>
    //   cfor(0)(_ < actualRows, _ + 1) { row =>
    //     actual.evaluate(new GridCoordinates2D(col, row), actualValues)
    //     expected.evaluate(new GridCoordinates2D(col, row), expectedValues)
    //     cfor(0)(_ < actualBandCount, _ + 1) { b =>
    //       val av = actualValues(b)
    //       val ev = expectedValues(b)
    //       withClue(s"ACTUAL: $av  EXPECTED: $ev  COL $col ROW $row BAND $b") {
    //         av should be (ev)
    //       }
    //     }
    //   }
    // }

    val actualImage = actual.getRenderedImage
    val expectedImage = expected.getRenderedImage

    val a = RectIterFactory.create(actualImage, null)
    val e = RectIterFactory.create(expectedImage, null)

    var break = false
    var (line, pixel, band) = (0, 0, 0)
    while(!e.finishedLines && !break) {
      a.finishedLines should be (false)
      while(!e.finishedPixels && !break) {
        a.finishedPixels should be (false)
        while(!e.finishedBands && !break) {
          a.finishedBands should be (false)
          withClue(s"Line $line Pixel $pixel Band $band") {
            a.getSampleFloat should be (e.getSampleFloat)
          }
          a.nextBand()
          break = e.nextBandDone()
          band += 1
        }
        band = 0
        a.finishedBands should be (true)
        a.nextPixel()
        e.startBands()
        a.startBands()
        break = e.nextPixelDone()
        pixel += 1
      }
      pixel = 0
      a.finishedPixels should be (true)
      a.nextLine()
      e.startPixels()
      a.startPixels()
      break = e.nextLineDone()
      line += 1
    }

    a.finishedLines should be (true)
  }

  for(testFile @ TestFile(description, path, isMultiband) <- testFilesWithProjections) {

    describe(s"ProjectedRaster Conversions: $description") {
      it("should convert the GridCoverage2D to a ProjectedRaster[MultibandTile]") {
        val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.multibandRaster)
        assertEqual(gridCoverage2D.toProjectedRaster, projectedRaster)
      }

      it("should convert a ProjectedRaster to a GridCoverage2D") {
        val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.multibandRaster)
        assertEqual(projectedRaster.toGridCoverage2D, gridCoverage2D)
      }
    }
  }

  for(testFile @ TestFile(description, path, isMultiband) <- testFilesWithProjections.filter(!_.isMultiband)) {
    val (gridCoverage2D, singlebandProjectedRaster) = (testFile.gridCoverage2D, testFile.singlebandRaster)

    describe(s"ProjectedRaster Conversions (singleband): $description") {
      it("should convert the GridCoverage2D to a ProjectedRaster[Tile]") {
        val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.singlebandRaster)

        assertEqual(gridCoverage2D.toProjectedRaster(0), projectedRaster)
      }

      it("should convert a ProjectedRaster[Tile] to a GridCoverage2D") {
        val (gridCoverage2D, projectedRaster) = (testFile.gridCoverage2D, testFile.singlebandRaster)
        assertEqual(projectedRaster.toGridCoverage2D, gridCoverage2D)
      }
    }
  }

  for(testFile @ TestFile(description, path, isMultiband) <- testFilesWithoutProjections) {

    describe(s"Raster Conversions: $description") {
      it("should convert the GridCoverage2D to a Raster[MultibandTile]") {
        val (gridCoverage2D, raster) = (testFile.gridCoverage2D, testFile.multibandRaster.raster)
        assertEqual(gridCoverage2D.toRaster, raster)
      }

      it("should convert a Raster to a GridCoverage2D") {
        val (gridCoverage2D, raster) = (testFile.gridCoverage2D, testFile.multibandRaster.raster)
        assertEqual(raster.toGridCoverage2D, gridCoverage2D)
      }
    }
  }

  for(testFile @ TestFile(description, path, isMultiband) <- testFilesWithoutProjections.filter(!_.isMultiband)) {
    val (gridCoverage2D, singlebandProjectedRaster) = (testFile.gridCoverage2D, testFile.singlebandRaster)

    describe(s"Raster Conversions (singleband): $description") {
      it("should convert the GridCoverage2D to a ProjectedRaster[Tile]") {
        val (gridCoverage2D, raster) = (testFile.gridCoverage2D, testFile.singlebandRaster.raster)

        assertEqual(gridCoverage2D.toRaster(0), raster)
      }

      it("should convert a ProjectedRaster[Tile] to a GridCoverage2D") {
        val (gridCoverage2D, raster) = (testFile.gridCoverage2D, testFile.singlebandRaster.raster)
        assertEqual(raster.toGridCoverage2D, gridCoverage2D)
      }
    }
  }

  for(testFile @ TestFile(description, path, isMultiband) <- testFilesGTandBackWithProjections ++ testFilesWithProjections.filter(!_.isMultiband)) {
    describe(s"Conversions to and from ProjectedRaster (singleband): $description") {
      it("should convert a ProjectedRaster[Tile] to a GridCoverage2D to a ProjectedRaster[Tile]") {
        val projectedRaster = testFile.singlebandRaster
        assertEqual(projectedRaster.toGridCoverage2D.toProjectedRaster(0), projectedRaster)
      }

      it("should convert a ProjectedRaster[MultibandTile] to a GridCoverage2D to a ProjectedRaster[MultibandTile]") {
        val projectedRaster = testFile.multibandRaster
        assertEqual(projectedRaster.toGridCoverage2D.toProjectedRaster, projectedRaster)
      }
    }
  }

  for(testFile @ TestFile(description, path, isMultiband) <- testFilesGTandBackWithoutProjections) {
    describe(s"Conversions to and from Raster (singleband): $description") {
      it("should convert a Raster[Tile] to a GridCoverage2D to a ProjectedRaster[Tile]") {
        val raster = testFile.singlebandRaster.raster
        assertEqual(raster.toGridCoverage2D.toRaster(0), raster)
      }

      it("should convert a Raster[MultibandTile] to a GridCoverage2D to a ProjectedRaster[MultibandTile]") {
        val raster = testFile.multibandRaster.raster
        assertEqual(raster.toGridCoverage2D.toRaster, raster)
      }
    }
  }
}
