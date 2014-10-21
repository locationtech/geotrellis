package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.engine._
import geotrellis.testkit._
import geotrellis.proj4._
import geotrellis.raster.io.geotiff.reader._

import org.scalatest._
import spire.syntax.cfor._

// TODO: Use the default crs in each tif?
class ReprojectSpec extends FunSpec
                       with TileBuilders
                       with TestEngine {
  describe("reprojects in approximation to GDAL") {
    // TODO: Implement Bilinear interpolation for reprojection
    it("should (approximately) match a GDAL bilinear interpolation on nlcd tile") {
      val (source, extent, _) = GeoTiffReader("raster-test/data/reproject/nlcd_tile_wsg84.tif").read.imageDirectories.head.toRaster

      val (expected, expectedExtent, _) = GeoTiffReader("raster-test/data/reproject/nlcd_tile_webmercator-bilinear.tif").read.imageDirectories.head.toRaster

      val (actual, actualExtent) = source.reproject(extent, LatLng, WebMercator, ReprojectOptions(Bilinear, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      for (i <- 0 until source.cols) {
        for (j <- 0 until source.rows) {
        if (source.get(i, j) < -30000) println(s"YOOOLO: ($i, $j) ${source.get(i, j)}")
        }
      }

      var diffCount = 0
      var notNoData = 0
      cfor(0)(_ < actual.rows, _ + 1) { row =>
        cfor(0)(_ < actual.cols, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row) +- 3000)
          }
          if (actual.getDouble(col, row) != expected.getDouble(col, row)) { diffCount += 1 }
          if (isData(actual.getDouble(col, row))) { notNoData += 1 }
        }
      }
      println(s"Total ${actual.cols * actual.rows}. Not No Data: $notNoData")
      diffCount should be (0)
    }

    it("should (approximately) match a GDAL nearest neighbor interpolation on nlcd tile") {
      val (source, extent, _) =
        GeoTiffReader("raster-test/data/reproject/nlcd_tile_wsg84.tif").read.imageDirectories.head.toRaster
      val (expected, expectedExtent, _) =
        GeoTiffReader("raster-test/data/reproject/nlcd_tile_webmercator-nearestneighbor.tif").read.imageDirectories.head.toRaster
      val (actual, actualExtent) =
        source.reproject(extent, LatLng, WebMercator, ReprojectOptions(NearestNeighbor, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows-1, _ + 1) { row =>
        cfor(0)(_ < actual.cols-1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }

    it("should (approximately) match a GDAL nearest neighbor interpolation on slope tif") {
      val (source, extent, _) =
        GeoTiffReader("raster-test/data/reproject/slope_webmercator.tif").read.imageDirectories.head.toRaster
      val (expected, expectedExtent, _) =
        GeoTiffReader("raster-test/data/reproject/slope_wsg84-nearestneighbor.tif").read.imageDirectories.head.toRaster
      val (actual, actualExtent) =
        source.reproject(extent, WebMercator, LatLng, ReprojectOptions(NearestNeighbor, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)

      cfor(0)(_ < actual.rows-1, _ + 1) { row =>
        cfor(0)(_ < actual.cols-1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }

    it("should (approximately) match a GDAL nearest neighbor interpolation on slope tif and an error threshold of 0.125") {
      val (source, extent, _) =
        GeoTiffReader("raster-test/data/reproject/slope_webmercator.tif").read.imageDirectories.head.toRaster
      val (expected, expectedExtent, _) =
        GeoTiffReader("raster-test/data/reproject/slope_wsg84-nearestneighbor-er0.125.tif").read.imageDirectories.head.toRaster
      val (actual, actualExtent) =
        source.reproject(extent, WebMercator, LatLng, ReprojectOptions(NearestNeighbor, 0.125))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)

      cfor(0)(_ < actual.rows-1, _ + 1) { row =>
        cfor(0)(_ < actual.cols-1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }
  }
}
