package geotrellis.graph.op

import geotrellis.graph._
import geotrellis.spark._
import geotrellis.spark.op.global._

import geotrellis.vector.Line

import geotrellis.raster._
import geotrellis.raster.op.global._

import org.scalatest.FunSpec

class ShortestPathSpec extends FunSpec with TestEnvironment
    with OpAsserter
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  ifCanRunSpark {

    val n = NODATA

    describe("Shortest Path Spec") {

      it("should perform as the single raster operation on raster #1") {
        val tile = ArrayTile(Array(
          1, 3, 4,  4, 3, 2,
          4, 6, 2,  3, 7, 6,
          5, 8, 7,  5, 6, 6,

          1, 4, 5,  n, 5, 1,
          4, 7, 5,  n, 2, 6,
          1, 2, 2,  1, 3, 4
        ), 6, 6)

        val points = Seq(
          (1, 0),
          (2, 0),
          (2, 1),
          (0, 5)
        )

        val rasterOp = (tile: Tile, re: RasterExtent) => tile.costDistance(points)

        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd.toGraph.costDistance(points)

        testTile(sc, tile, 3, 3)(rasterOp, sparkOp)
      }

      it("should perform as the single raster operation on raster #2") {
        val tile = ArrayTile(Array(
          n, 7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, n ,1,   8, 1, 1
        ), 9, 4)

        val points = Seq(
          (1, 0),
          (2, 0),
          (3, 3),
          (8, 2)
        )

        val rasterOp = (tile: Tile, re: RasterExtent) => tile.costDistance(points)

        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd.toGraph.costDistance(points)

        testTile(sc, tile, 3, 2)(rasterOp, sparkOp)
      }

      it("should perform as the single raster operation on raster #3") {
        val tile = ArrayTile(Array(
          2 , 2 , 1 , 1 , 5 , 5 , 5 ,
          2 , 2 , 8 , 8 , 5 , 2 , 1 ,
          7 , 1 , 1 , 8 , 2 , 2 , 2 ,
          8 , 7 , 8 , 8 , 8 , 8 , 5 ,
          8 , 8 , 1 , 1 , 5 , 3 , 9 ,
          8 , 1 , 1 , 2 , 5 , 3 , 9), 7, 6)

        val points = Seq((5, 4))

        val rasterOp = (tile: Tile, re: RasterExtent) => tile.costDistance(points)

        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd.toGraph.costDistance(points)

        testTile(sc, tile, 2, 3)(rasterOp, sparkOp)
      }

    }

    describe("Shortest Path With Path Recreation Spec") {

      it("should recreate path on a simple raster #1") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            1,   100, 100,  100, 100, 100,  100, 100, 100,
            100, 1,   100,  100, 100, 100,  100, 100, 100,
            100, 100, 1,    100, 100, 100,  100, 100, 100,
            100, 100, 100,  1,   100, 100,  100, 100, 100,
            100, 100, 100,  100, 1,   100,  100, 100, 100,
            100, 100, 100,  100, 100, 1,    100, 100, 100,
            100, 100, 100,  100, 100, 100,  1,   100, 100,
            100, 100, 100,  100, 100, 100,  100, 1,   100,
            100, 100, 100,  100, 100, 100,  100, 100, 1
          ), 9, 9),
          TileLayout(3, 3, 3, 3)
        )

        val start = (0, 0)
        val end = (8, 8)

        val cheapestPath = Line(Seq(
          (0.0, 0.0), (1.0, 1.0), (2.0, 2.0),
          (3.0, 3.0), (4.0, 4.0), (5.0, 5.0),
          (6.0, 6.0), (7.0, 7.0), (8.0, 8.0)
        ))

        val paths = rasterRDD.toGraph.costDistanceWithPath(start, end)
        paths.size should be (1)
        paths.head should be (cheapestPath)
      }

      it("should recreate path on a simple raster #2") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            1,   1,   1,    1,   1,   1,    1,   1,   1,
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            1,   1,   1,    1,   1,   1,    1,   1,   1
          ), 9, 9),
          TileLayout(3, 3, 3, 3)
        )

        val start = (0, 0)
        val end = (8, 8)

        val cheapestPaths = Set(
          Line(Seq(
            (0.0, 0.0), (0.0, 1.0), (0.0, 2.0),
            (0.0, 3.0), (0.0, 4.0), (0.0, 5.0),
            (0.0, 6.0), (0.0, 7.0), (1.0, 8.0),
            (2.0, 8.0), (3.0, 8.0), (4.0, 8.0),
            (5.0, 8.0), (6.0, 8.0), (7.0, 8.0),
            (8.0, 8.0)
          )),
          Line(Seq(
            (0.0, 0.0), (1.0, 0.0), (2.0, 0.0),
            (3.0, 0.0), (4.0, 0.0), (5.0, 0.0),
            (6.0, 0.0), (7.0, 0.0), (8.0, 1.0),
            (8.0, 2.0), (8.0, 3.0), (8.0, 4.0),
            (8.0, 5.0), (8.0, 6.0), (8.0, 7.0),
            (8.0, 8.0)
          ))
        )

        val paths = rasterRDD.toGraph.costDistanceWithPath(start, end)
        paths.size should be (2)
        paths.toSet should be (cheapestPaths)
      }

      it("should recreate path on a simple raster #3") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            1,   100, 100,  100, 100, 100,  100, 100, 1,
            100, 1,   100,  100, 100, 100,  100, 1  , 1,
            100, 100, 1,    100, 100, 100,  1,   100, 1,
            100, 100, 100,  1,   100, 1,    100, 100, 1,
            100, 100, 100,  100, 1,   100,  100, 100, 1,
            100, 100, 100,  1,   100, 100,  100, 100, 1,
            100, 100, 1,    100, 100, 100,  n,   100, 1,
            100, 1,   100,  100, 100, 100,  100, 100, 1,
            1,   1,   1,    1,   1,   1,    1,   1,   1
          ), 9, 9),
          TileLayout(3, 3, 3, 3)
        )

        val start = (0, 0)
        val end = (8, 8)

        val cheapestPaths = Set(
          Line(Seq(
            (0.0, 0.0), (1.0, 1.0), (2.0, 2.0),
            (3.0, 3.0), (4.0, 4.0), (3.0, 5.0),
            (2.0, 6.0), (1.0, 7.0), (2.0, 8.0),
            (3.0, 8.0), (4.0, 8.0), (5.0, 8.0),
            (6.0, 8.0), (7.0, 8.0), (8.0, 8.0)
          )),
          Line(Seq(
            (0.0, 0.0), (1.0, 1.0), (2.0, 2.0),
            (3.0, 3.0), (4.0, 4.0), (5.0, 3.0),
            (6.0, 2.0), (7.0, 1.0), (8.0, 2.0),
            (8.0, 3.0), (8.0, 4.0), (8.0, 5.0),
            (8.0, 6.0), (8.0, 7.0), (8.0, 8.0)
          ))
        )

        val paths = rasterRDD.toGraph.costDistanceWithPath(start, end)
        paths.size should be (2)
        paths.toSet should be (cheapestPaths)
      }

    }

  }

}
