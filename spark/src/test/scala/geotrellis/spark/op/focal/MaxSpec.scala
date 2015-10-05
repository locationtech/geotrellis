package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster.op.focal._
import geotrellis.raster._

import org.scalatest.FunSpec

class MaxSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Max Focal Spec") {

    ifCanRunSpark {

      val nd = NODATA

      it("should square max for raster rdd") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd,7, 1,   1, 1, 1,   1, 1, 1,
            9, 1, 1,   2, 2, 2,   1, 3, 1,

            3, 8, 1,   3, 3, 3,   1, 1, 2,
            2, 1, 7,   1, nd,1,   8, 1, 1
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalMax(Square(1)).stitch.tile.toArray

        val expected = Array(
          9, 9, 7,    2, 2, 2,    3, 3, 3,
          9, 9, 8,    3, 3, 3,    3, 3, 3,

          9, 9, 8,    7, 3, 8,    8, 8, 3,
          8, 8, 8,    7, 3, 8,    8, 8, 2
        )

        res should be(expected)
      }

      it("should square max with 5 x 5 neighborhood") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd,7, 1,   1, 1, 1,   1, 1, 1,
            9, 1, 1,   2, 2, 2,   1, 3, 1,

            3, 8, 1,   3, 3, 3,   1, 1, 2,
            2, 1, 7,   1, nd,1,   8, 1, 1
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalMax(Square(2)).stitch.tile.toArray

        val expected = Array(
          9, 9, 9,    8, 3, 3,    3, 3, 3,
          9, 9, 9,    8, 8, 8,    8, 8, 8,

          9, 9, 9,    8, 8, 8,    8, 8, 8,
          9, 9, 9,    8, 8, 8,    8, 8, 8
        )

        res should be(expected)
      }

      it("should circle max for raster rdd") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd,7, 1,   1, 1, 1,   1, 1, 1,
            9, 1, 1,   2, 2, 2,   1, 3, 1,

            3, 8, 1,   3, 3, 3,   1, 1, 2,
            2, 1, 7,   1, nd,1,   8, 1, 1
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalMax(Circle(1)).stitch.tile.toArray

        val expected = Array(
          9, 7, 7,    2, 2, 2,    1, 3, 1,
          9, 9, 2,    3, 3, 3,    3, 3, 3,

          9, 8, 8,    3, 3, 3,    8, 3, 2,
          3, 8, 7,    7, 3, 8,    8, 8, 2
        )

        res should be (expected)
      }

    }
  }
}
