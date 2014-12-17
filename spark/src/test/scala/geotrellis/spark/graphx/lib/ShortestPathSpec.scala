package geotrellis.spark.graphx.lib

import geotrellis.spark._
import geotrellis.spark.graphx._
import geotrellis.spark.graphx.lib._

import geotrellis.raster._
import geotrellis.raster.op.global._

import org.scalatest.FunSpec

class ShortestPathSpec extends FunSpec with TestEnvironment
    with OpAsserter
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Shortest Path Spec") {

    ifCanRunSpark {

      val n = NODATA

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

        val rasterOp = (tile: Tile, re: RasterExtent) => CostDistance(tile, points)

        val sparkOp = (rdd: RasterRDD[SpatialKey]) =>
        rdd
          .toGraph
          .shortestPath(points)
          .toRaster

        testTile(sc, tile, 3, 3)(rasterOp, sparkOp)
      }

      // TODO: something is wierd. Either CostDistance for rasters, or test framework or graphx algo.
      ignore("should perform as the single raster operation on raster #2") {
        val tile = ArrayTile(Array(
          n, 7, 1,   1, 1, 1,   1, 1, 1,
          9, 1, 1,   2, 2, 2,   1, 3, 1,

          3, 8, 1,   3, 3, 3,   1, 1, 2,
          2, 1, 7,   1, n ,1,   8, 1, 1
        ), 9, 4)

        val points = Seq(
          (1, 0)//,
                //(2, 0)
                //(3, 3),
                //(8, 2)
        )

        val rasterOp = (tile: Tile, re: RasterExtent) => CostDistance(tile, points)

        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd
          .toGraph
          .shortestPath(points)
          .toRaster

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

        val rasterOp = (tile: Tile, re: RasterExtent) => CostDistance(tile, points)

        val sparkOp = (rdd: RasterRDD[SpatialKey]) => rdd
          .toGraph
          .shortestPath(points)
          .toRaster

        testTile(sc, tile, 7, 6)(rasterOp, sparkOp)
      }

    }

  }




}
