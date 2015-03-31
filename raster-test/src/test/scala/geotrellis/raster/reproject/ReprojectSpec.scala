package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.interpolation._
import geotrellis.engine._
import geotrellis.testkit._
import geotrellis.proj4._
import geotrellis.raster.interpolation._
import geotrellis.raster.io.geotiff.reader._

import org.scalatest._
import spire.syntax.cfor._

class ReprojectSpec extends FunSpec
    with TileBuilders
    with TestEngine {
  describe("reprojects in approximation to GDAL") {

    it("should (approximately) match a GDAL nearest neighbor interpolation on nlcd tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_webmercator-nearestneighbor.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator, ReprojectOptions(NearestNeighbor, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL bilinear interpolation on nlcd tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_webmercator-bilinear.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator, ReprojectOptions(Bilinear, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL cubic convolution interpolation on nlcd tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_webmercator-cubic_convolution.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator, ReprojectOptions(CubicConvolution, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL cubic spline interpolation on nlcd tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_webmercator-cubic_spline.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator, ReprojectOptions(CubicSpline, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL lanczos interpolation on nlcd tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_webmercator-lanczos.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator, ReprojectOptions(Lanczos, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL average interpolation on nlcd tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_webmercator-average.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator, ReprojectOptions(Average, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL mode interpolation on nlcd tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_webmercator-mode.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator, ReprojectOptions(Mode, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL nearest neighbor on nlcd lat lon tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_latlon-nearestneighbor.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, LatLng, ReprojectOptions(NearestNeighbor, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL bilinear on nlcd lat lon tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_latlon-bilinear.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, LatLng, ReprojectOptions(Bilinear, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL cubic convolution interpolation on nlcd lat lon tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_latlon-cubic_convolution.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, LatLng, ReprojectOptions(CubicConvolution, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL cubic spline interpolation on nlcd lat lon tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_latlon-cubic_spline.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, LatLng, ReprojectOptions(CubicSpline, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL lanczos interpolation on nlcd lat lon tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_latlon-lanczos.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, LatLng, ReprojectOptions(Average, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL average interpolation on nlcd lat lon tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_latlon-average.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, LatLng, ReprojectOptions(Average, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL mode interpolation on nlcd lat lon tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_latlon-mode.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, LatLng, ReprojectOptions(Mode, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL nearest neighbor on nlcd utm tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_utm-nearestneighbor.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, CRS.fromName("EPSG:32614"), ReprojectOptions(NearestNeighbor, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL bilinear on nlcd utm tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_utm-bilinear.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, CRS.fromName("EPSG:32614"), ReprojectOptions(Bilinear, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL cubic convolution interpolation on nlcd utm tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_utm-cubic_convolution.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, CRS.fromName("EPSG:32614"), ReprojectOptions(CubicConvolution, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL cubic spline interpolation on nlcd utm tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_utm-cubic_spline.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, CRS.fromName("EPSG:32614"), ReprojectOptions(CubicSpline, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL lanczos interpolation on nlcd utm tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_utm-lanczos.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, CRS.fromName("EPSG:32614"), ReprojectOptions(Average, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL average interpolation on nlcd utm tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_utm-average.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, CRS.fromName("EPSG:32614"), ReprojectOptions(Average, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }


    it("should (approximately) match a GDAL mode interpolation on nlcd utm tile") {
      val GeoTiffBand(source, extent, crs, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_wsg84.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/nlcd_tile_utm-mode.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, crs, CRS.fromName("EPSG:32614"), ReprojectOptions(Mode, 0.0))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }
    it("should (approximately) match a GDAL nearest neighbor interpolation on slope tif and an error threshold of 0.125") {
      val GeoTiffBand(source, extent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/slope_webmercator.tif")
        .firstBand

      val GeoTiffBand(expected, expectedExtent, _, _) = GeoTiffReader
        .read("raster-test/data/reproject/slope_wsg84-nearestneighbor-er0.125.tif")
        .firstBand

      val Raster(actual, actualExtent) =
        source.reproject(extent, WebMercator, LatLng, ReprojectOptions(NearestNeighbor, 0.125))

      actual.rows should be (expected.rows)
      actual.cols should be (expected.cols)

      actualExtent.xmin should be (expectedExtent.xmin +- 0.00001)
      actualExtent.xmax should be (expectedExtent.xmax +- 0.00001)
      actualExtent.ymax should be (expectedExtent.ymax +- 0.00001)
      actualExtent.ymin should be (expectedExtent.ymin +- 0.00001)

      cfor(0)(_ < actual.rows - 1, _ + 1) { row =>
        cfor(0)(_ < actual.cols - 1, _ + 1) { col =>
          withClue(s"Failed on ($col, $row): ") {
            actual.getDouble(col, row) should be (expected.getDouble(col, row))
          }
        }
      }
    }
  }
}
