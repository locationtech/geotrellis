package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.raster.op.focal._
import geotrellis.raster._

import org.scalatest.FunSpec

class MinSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with RasterRDDBuilders {

  describe("Min Focal Spec") {

    val nd = NODATA

    val NaN = Double.NaN

    it("should square min for raster rdd") {
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

      val res = rasterRDD.focalMin(Square(1)).stitch.tile.toArray

      val expected = Array(
        1, 1, 1,    1, 1, 2,    2, 2, 2,
        1, 1, 1,    1, 1, 1,    1, 1, 2,

        1, 1, 1,    1, 1, 1,    1, 1, 2,
        2, 1, 1,    1, 1, 1,    1, 1, 2
      )

      res should be(expected)
    }

    it("should square min for raster rdd with doubles") {
      val rasterRDD = createRasterRDD(
        sc,
        ArrayTile(Array(
          NaN, 7.1, 1.2,   1.4, 3.9, 5.1,   9.9, 8.1, 2.2,
          9.4, 1.1, 1.5,   2.5, 2.2, 2.9,   4.0, 3.3, 5.1,

          3.4, 8.2, 1.9,   3.8, 3.1, 3.0,   1.3, 2.1, 2.5,
          2.5, 4.9, 7.1,   1.4, NaN, 1.1,   8.0, 4.8, 3.0
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMin(Square(1)).stitch.tile.toArrayDouble

      val expected = Array(
        1.1, 1.1, 1.1,    1.2, 1.4, 2.2,    2.9, 2.2, 2.2,
        1.1, 1.1, 1.1,    1.2, 1.4, 1.3,    1.3, 1.3, 2.1,

        1.1, 1.1, 1.1,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1,
        2.5, 1.9, 1.4,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1
      )

      res should be(expected)
    }

    it("should square min with 5 x 5 neighborhood") {
      val rasterRDD = createRasterRDD(
        sc,
        ArrayTile(Array(
          nd,7, 7,   7, 3, 5,   9, 8, 2,
          9, 7, 7,   2, 2, 2,   4, 3, 5,

          3, 8, 7,   3, 3, 3,   7, 4, 5,
          9, 4, 7,   7,nd, 7,   8, 4, 3
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMin(Square(2)).stitch.tile.toArray

      val expected = Array(
        3, 2, 2,    2, 2, 2,    2, 2, 2,
        3, 2, 2,    2, 2, 2,    2, 2, 2,

        3, 2, 2,    2, 2, 2,    2, 2, 2,
        3, 2, 2,    2, 2, 2,    2, 2, 3
      )

      res should be(expected)
    }

    it("should circle min for raster rdd") {
      val rasterRDD = createRasterRDD(
        sc,
        ArrayTile(Array(
          nd,7, 4,   5, 4, 2,   9,nd,nd,
          9, 6, 2,   2, 2, 2,   5, 3,nd,

          3, 8, 4,   3, 3, 3,   3, 9, 2,
          2, 9, 7,   4,nd, 9,   8, 8, 4
        ), 9, 4),
        TileLayout(3, 2, 3, 2)
      )

      val res = rasterRDD.focalMin(Circle(1)).stitch.tile.toArray

      val expected = Array(
        7, 4, 2,    2, 2, 2,    2, 3, nd,
        3, 2, 2,    2, 2, 2,    2, 3, 2,

        2, 3, 2,    2, 2, 2,    3, 2, 2,
        2, 2, 4,    3, 3, 3,    3, 4, 2
      )

      res should be (expected)
    }
  }
}
