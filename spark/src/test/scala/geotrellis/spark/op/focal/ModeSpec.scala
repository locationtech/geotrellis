package geotrellis.spark.op.focal

import geotrellis.spark._

import geotrellis.raster.op.focal._
import geotrellis.raster._

import org.scalatest.FunSpec

class ModeSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with TestSparkContext
    with RasterRDDBuilders {

  describe("Mode Focal Spec") {

    val nd = NODATA

    it("should square mode for raster rdd") {
      val rasterRDD = createRasterRDD(
        sc,
        ArrayTile(Array(
          nd,7, 1,   1, 3, 5,   9, 8, 2,
          9, 1, 1,   2, 2, 2,   4, 3, 5,

          3, 8, 1,   3, 3, 3,   1, 2, 2,
          2, 4, 7,   1,nd, 1,   8, 4, 3
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMode(Square(1)).stitch.tile.toArray

      val expected = Array(
        nd, 1, 1,    1, 2, 2,   nd,nd,nd,
        nd, 1, 1,    1, 3, 3,   nd, 2, 2,

        nd, 1, 1,    1,nd,nd,   nd,nd,nd,
        nd,nd, 1,   nd, 3,nd,    1, 2, 2
      )

      res should be (expected)
    }

  }
}
