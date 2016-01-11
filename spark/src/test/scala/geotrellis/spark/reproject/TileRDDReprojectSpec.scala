package geotrellis.spark.reproject

import geotrellis.spark._
import geotrellis.spark.tiling._

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.resample._
import geotrellis.raster.reproject._
import geotrellis.raster.reproject.Reproject.Options
import geotrellis.vector._
import geotrellis.vector.io.json._
import geotrellis.vector.reproject._

import geotrellis.proj4._

import spire.syntax.cfor._
import org.scalatest.FunSpec

class TileRDDReprojectSpec extends FunSpec
    with TestEnvironment
    with RasterRDDBuilders 
    with RasterMatchers {

  describe("TileRDDReproject") {
    val path = "raster-test/data/aspect.tif"
    val gt = SingleBandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)

    val (raster, rdd) = {
      val (raster, rdd) = createRasterRDD(originalRaster, 10, 10, gt.crs)
      (raster, rdd.withContext { rdd => rdd.repartition(20) })
    }

    def testReproject(method: ResampleMethod, constantBuffer: Boolean): Unit = {
      val expected = ProjectedRaster(raster, gt.crs).reproject(LatLng, Options(method = method, errorThreshold = 0))
      val (_, actualRdd) =
        if(constantBuffer) {
          rdd.reproject(LatLng, FloatingLayoutScheme(25), bufferSize = 2, Options(method = method, errorThreshold = 0))
        } else {
          rdd.reproject(LatLng, FloatingLayoutScheme(25), Options(method = method, errorThreshold = 0))
        }

      val actual =
        actualRdd.stitch

      // Account for tiles being a bit bigger then the actual result
      actual.extent.covers(expected.extent) should be (true)
      actual.rasterExtent.extent.xmin should be (expected.rasterExtent.extent.xmin +- 0.00001)
      actual.rasterExtent.extent.ymax should be (expected.rasterExtent.extent.ymax +- 0.00001)
      actual.rasterExtent.cellwidth should be (expected.rasterExtent.cellwidth +- 0.00001)
      actual.rasterExtent.cellheight should be (expected.rasterExtent.cellheight +- 0.00001)

      val expectedTile = expected.tile
      val actualTile = actual.tile

      actualTile.cols should be >= (expectedTile.cols)
      actualTile.rows should be >= (expectedTile.rows)

      cfor(0)(_ < actual.rows, _ + 1) { row =>
        cfor(0)(_ < actual.cols, _ + 1) { col =>
          val a = actualTile.getDouble(col, row)
          if(row >= expectedTile.rows || col >= expectedTile.cols) {
            isNoData(a) should be (true)
          } else if(row != 1){
            val expected = expectedTile.getDouble(col, row)
            if (a.isNaN) {
              withClue(s"Failed at col: $col and row: $row, $a != $expected") {
                expected.isNaN should be (true)
              }
            } else if (expected.isNaN) {
              withClue(s"Failed at col: $col and row: $row, $a != $expected") {
                a.isNaN should be (true)
              }
            } else {
              withClue(s"Failed at col: $col and row: $row, $a != $expected") {
                a should be (expected +- 0.001)
              }
            }
          }
        }
      }
    }

    it("should reproject a raster split into tiles the same as the raster itself: constant border and Bilinear") {
      testReproject(Bilinear, true)
    }

    it("should reproject a raster split into tiles the same as the raster itself: dynamic border and Bilinear") {
      testReproject(Bilinear, false)
    }

    it("should reproject a raster split into tiles the same as the raster itself: constant border and NearestNeighbor") {
      testReproject(NearestNeighbor, true)
    }

    it("should reproject a raster split into tiles the same as the raster itself: dynamic border and NearestNeighbor") {
      testReproject(NearestNeighbor, false)
    }
  }
}
