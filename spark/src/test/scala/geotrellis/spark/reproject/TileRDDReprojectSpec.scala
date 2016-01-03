package geotrellis.spark.reproject

import geotrellis.spark._
import geotrellis.spark.tiling._

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.resample._
import geotrellis.proj4._

import org.scalatest.FunSpec

class TileRDDReprojectSpec extends FunSpec
    with TestEnvironment
    with TestSparkContext
    with RasterRDDBuilders 
    with RasterMatchers {

  describe("TileRDDReproject") {
    it("should reproject a large raster split into tiles the same as the raster itself") {
      val path = "raster-test/data/aspect.tif"
      val gt = SingleBandGeoTiff(path)

      val (tile, rdd) = createRasterRDD(gt.tile, 50, 50, gt.crs)

      val extent =
        gt.raster.rasterExtent.extentFor(GridBounds(0, 0, tile.cols - 1, tile.rows - 1))

      val raster = Raster(tile, extent)

      val expected = ProjectedRaster(raster, gt.crs).reproject(LatLng)

      val actual = rdd.reproject(LatLng, FloatingLayoutScheme(20))._2.stitch

      tilesEqual(actual, expected)
    }
  }
}
