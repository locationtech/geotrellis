package geotrellis.spark.op.focal

import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.op.focal._
import geotrellis.spark.testkit._
import geotrellis.spark._
import geotrellis.spark.op.focal._
import org.scalatest._

class SumSpec extends FunSpec
    with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Sum Focal Spec") {

    ifCanRunSpark {

      val nd = NODATA

      it("should square sum r = 1 for raster rdd") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd,1, 1,   1, 1, 1,   1, 1, 1,
            1, 1, 1,   2, 2, 2,   1, 1, 1,

            1, 1, 1,   3, 3, 3,   1, 1, 1,
            1, 1, 1,   1,nd, 1,   1, 1, 1
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalSum(Square(1)).stitch.toArray

        val expected = Array(
          3, 5, 7,    8, 9, 8,    7, 6, 4,
          5, 8,12,   15,18,15,   12, 9, 6,

          6, 9,12,   14,17,14,   12, 9, 6,
          4, 6, 8,    9,11, 9,    8, 6, 4
        )

        res should be (expected)
      }

      it("should square sum with 5x5 neighborhood") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd,1, 1,   1, 1, 1,   1, 1, 1,
            1, 1, 1,   2, 2, 2,    1, 1, 1,

            1, 1, 1,   3, 3, 3,    1, 1, 1,
            1, 1, 1,   1,nd, 1,    1, 1, 1
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalSum(Square(2)).stitch.toArray

        val expected = Array(
          8, 14, 20,   24,24,24,    21,15, 9,
          11, 18, 24,  28,28,28,   25,19,12,

          11, 18, 24,  28,28,28,    25,19,12,
          9, 15, 20,   23,23,23,    20,15, 9
        )

        res should be (expected)
      }

      it("should circle sum for raster source") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd,1, 1,   1, 1, 1,   1, 1, 1,
            1, 1, 1,   2, 2, 2,    1, 1, 1,

            1, 1, 1,   3, 3, 3,    1, 1, 1,
            1, 1, 1,   1,nd, 1,    1, 1, 1
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        val res = rasterRDD.focalSum(Circle(1)).stitch.toArray

        val expected = Array(
          2, 3, 4,    5, 5, 5,    4, 4, 3,
          3, 5, 6,    9,10, 9,    6, 5, 4,

          4, 5, 7,   10,11,10,    7, 5, 4,
          3, 4, 4,    5, 5, 5,    4, 4, 3
        )

        res should be (expected)
      }
    }
  }
}
