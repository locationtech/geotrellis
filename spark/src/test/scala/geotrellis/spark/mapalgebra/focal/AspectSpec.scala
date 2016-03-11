package geotrellis.spark.mapalgebra.focal

import geotrellis.raster._
import geotrellis.spark._
import org.scalatest.FunSpec

class AspectSpec extends FunSpec with TestEnvironment {

  describe("Aspect Elevation Spec") {

    it("should match gdal computed slope raster") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.aspect(re.cellSize)
      val sparkOp = (rdd: TileLayerRDD[SpatialKey]) => rdd.aspect()

      val path = "aspect.tif"

      testGeoTiff(sc, path)(rasterOp, sparkOp)
    }

  }
}
