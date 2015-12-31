package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.raster.mosaic._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.vector.io.json._
import geotrellis.engine._
import geotrellis.testkit._
import geotrellis.proj4._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._

import org.scalatest._
import spire.syntax.cfor._

class ReprojectSpec extends FunSpec
    with TileBuilders
    with GeoTiffTestUtils
    with TestEngine {
  describe("reprojects in approximation to GDAL") {

    it("should (approximately) match a GDAL nearest neighbor interpolation on nlcd tile") {
      val ProjectedRaster(source, extent, crs) = SingleBandGeoTiff("raster-test/data/reproject/nlcd_tile_wsg84.tif").projectedRaster

      val ree @ Raster(expected, expectedExtent) =
        SingleBandGeoTiff("raster-test/data/reproject/nlcd_tile_webmercator-nearestneighbor.tif").raster
 
      val rea @ Raster(actual, actualExtent) =
        source.reproject(extent, crs, WebMercator)(method = NearestNeighbor, errorThreshold = 0.0)

      // println(ree.rasterExtent)
      // println(rea.rasterExtent)
      // actual.rows should be (expected.rows)
      // actual.cols should be (expected.cols)

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

    it("should (approximately) match a GDAL nearest neighbor interpolation on slope tif") {
      val Raster(source, extent) = 
        SingleBandGeoTiff("raster-test/data/reproject/slope_webmercator.tif").raster

      val Raster(expected, expectedExtent) = 
        SingleBandGeoTiff("raster-test/data/reproject/slope_wsg84-nearestneighbor.tif").raster

      val Raster(actual, actualExtent) =
        source.reproject(extent, WebMercator, LatLng)(method = NearestNeighbor, errorThreshold = 0.0)

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

    it("should (approximately) match a GDAL nearest neighbor interpolation on slope tif and an error threshold of 0.125") {
      val Raster(source, extent) = 
        SingleBandGeoTiff("raster-test/data/reproject/slope_webmercator.tif").raster

      val Raster(expected, expectedExtent) = 
        SingleBandGeoTiff("raster-test/data/reproject/slope_wsg84-nearestneighbor-er0.125.tif").raster

      val Raster(actual, actualExtent) =
        source.reproject(extent, WebMercator, LatLng)(method = NearestNeighbor, errorThreshold = 0.124)

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

    it("should reproject two landsat tiles into rasters that don't have nodata lines of NODATA") {
      def detectNoDataLine(tile: Tile): Unit = {
        val (cols, rows) = tile.dimensions
        val noDataColCounts = Array.ofDim[Int](cols)
        cfor(0)(_ < rows, _ + 1) { row =>
          cfor(0)(_ < cols, _ + 1) { col =>
            if(isNoData(tile.get(col, row))) {
              noDataColCounts(col) += 1
              if(noDataColCounts(col) > 50) {
                sys.error(s"No data line detected at column $col")
              }
            } else {
              noDataColCounts(col) = 0
            }
          }
        }
      }

      val srcCRS = CRS.fromEpsgCode(32618)

      val leftRasterExtent = RasterExtent(Extent(563760.000, 4428900.000, 579120.000, 4444260.000), 30.0, 30.0, 512, 512)
      val rightRasterExtent = RasterExtent(Extent(579120.000, 4428900.000, 594480.000, 4444260.000), 30.0, 30.0, 512, 512)

      val leftTile = IntArrayTile(Array.ofDim[Int](256 * 256).fill(1), 256, 256)

      val rightTile = IntArrayTile(Array.ofDim[Int](256 * 256).fill(2), 256, 256)

      // Sanity check - they don't have any missing pixels before reprojecting
      val mergedRaster = {
        val RasterExtent(_, cellwidth, cellheight, _, _) = leftRasterExtent
        val unionExtent = leftRasterExtent.extent.combine(rightRasterExtent.extent)
        val re = RasterExtent(unionExtent, CellSize(cellwidth, cellheight))
        val mergeTile = ArrayTile.empty(TypeInt, re.cols, re.rows)
        mergeTile.merge(unionExtent, leftRasterExtent.extent, leftTile)
        mergeTile.merge(unionExtent, rightRasterExtent.extent, rightTile)
        detectNoDataLine(mergeTile)
        Raster(mergeTile, unionExtent)
      }

      // Now repreject; there should also be no lines.

      val wmLeft @ Raster(wmLeftTile, wmLeftExtent) = 
        mergedRaster.reproject(GridBounds(0, 0, 511, 1023), srcCRS, WebMercator)(method = Bilinear)

      val wmRight @ Raster(wmRightTile, wmRightExtent) = 
        mergedRaster.reproject(GridBounds(512, 0, 1023, 1023), srcCRS, WebMercator)(method = Bilinear)

      // TODO: Remove
      // GeoTiff(mergedRaster, srcCRS).write("/Users/rob/tmp/reproject-bugs/merged-src-raster.tif")
      // GeoTiff(mergedRaster.reproject(srcCRS, WebMercator)(method = Bilinear), WebMercator).write("/Users/rob/tmp/reproject-bugs/merged-reprojected-raster.tif")
      // GeoTiff(wmLeft, WebMercator).write("/Users/rob/tmp/reproject-bugs/reproject-left-nomerge.tif")
      // GeoTiff(wmRight, WebMercator).write("/Users/rob/tmp/reproject-bugs/reproject-right-nomerge.tif")

      val rel @ RasterExtent(_, cellwidthLeft, cellheightLeft, _, _) = RasterExtent(wmLeftExtent, wmLeftTile.cols, wmLeftTile.rows)
      val rer @ RasterExtent(_, cellwidthRight, cellheightRight, _, _) = RasterExtent(wmRightExtent, wmRightTile.cols, wmRightTile.rows)

      cellwidthLeft should be (cellwidthRight +- 0.01)
      cellheightLeft should be (cellheightRight +- 0.01)

      // Specifically fit it ito a web mercator zoom layout tile
      val re = RasterExtent(Extent(-8247861.100, 4872401.931, -8238077.160, 4882185.871), 256, 256)

      val emptyTile = ArrayTile.empty(TypeInt, re.cols, re.rows)
      val mergeTile: Tile = emptyTile.merge(re.extent, wmLeftExtent, wmLeftTile).merge(re.extent, wmRightExtent, wmRightTile)

      def doAll() = {
        val tile = IntArrayTile(Array.ofDim[Int](re.cols * re.rows).fill(3), re.cols, re.rows)
        GeoTiff(tile, re.extent, WebMercator).write("/Users/rob/tmp/reproject-bugs/target-wm-tile.tif")
      }
      doAll()

      def doLeft() = {
        val emptyTile = ArrayTile.empty(TypeInt, re.cols, re.rows)
        val mergeTile: Tile = emptyTile.merge(re.extent, wmLeftExtent, wmLeftTile)
        GeoTiff(mergeTile, re.extent, WebMercator).write("/Users/rob/tmp/reproject-bugs/merged-from-window-reproject-left.tif")
      }
      doLeft()

      def doRight() = {
        val emptyTile = ArrayTile.empty(TypeInt, re.cols, re.rows)
        val mergeTile: Tile = emptyTile.merge(re.extent, wmRightExtent, wmRightTile)
        GeoTiff(mergeTile, re.extent, WebMercator).write("/Users/rob/tmp/reproject-bugs/merged-from-window-reproject-right.tif")
      }
      doRight()



      GeoTiff(mergeTile, re.extent, WebMercator).write("/Users/rob/tmp/reproject-bugs/merged-from-window-reproject.tif")

      detectNoDataLine(mergeTile)
    }

    it("should project to the same extent when from a window of a larger raster than when projecting that raster with no window") {
      val srcCRS = CRS.fromEpsgCode(32618)
      val destCRS = WebMercator

      val rasterExtent = RasterExtent(Extent(563760.000, 4428900.000, 579120.000, 4444260.000), 30.0, 30.0, 512, 512)

      val expandedGridBounds = GridBounds(-10, -10, rasterExtent.cols + 10 - 1, rasterExtent.rows + 10 - 1)
      val expandedExtent = rasterExtent.extentFor(expandedGridBounds, clamp = false)
      val expandedRasterExtent = RasterExtent(expandedExtent, rasterExtent.cols + 20, rasterExtent.rows + 20)

      val expandedTile = 
        IntArrayTile(Array.ofDim[Int](expandedRasterExtent.size).fill(1), expandedRasterExtent.cols, expandedRasterExtent.rows)
      val expandedRaster = Raster(expandedTile, expandedExtent)

      val tile = IntArrayTile(Array.ofDim[Int](rasterExtent.size).fill(1), rasterExtent.cols, rasterExtent.rows)
      val raster = Raster(tile, rasterExtent.extent)

      val windowBounds = GridBounds(10, 10, 10 + rasterExtent.cols - 1, 10 + rasterExtent.rows - 1)

      val regularReproject = raster.reproject(srcCRS, destCRS)
      val windowedReproject = expandedRaster.reproject(windowBounds, srcCRS, destCRS)

      windowedReproject.extent should be (regularReproject.extent)
    }
  }
}
