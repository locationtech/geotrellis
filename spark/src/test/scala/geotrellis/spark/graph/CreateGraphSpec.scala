package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.spark.graph._

import geotrellis.raster._

import org.scalatest.FunSpec

class CreateGraphSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Create Graph Spec") {

    ifCanRunSpark {

      val nd = NODATA

      it("should create a correct graph for the given rdd #1") {
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

        val graph = rasterRDD.toGraph

        graph.numVertices should be (9 * 4)

        graph.numEdges should be (107)

        val groupedVertices = graph.vertices
          .groupBy(_._2)
          .map { case(key, iter) => (key, iter.map(_._1).toSeq) }
          .collect

        val base = Seq(0, 1, 2, 3, 4, 5)
        var coordinatesSet = Set((0, 0), (0, 1), (0, 2), (1, 0), (1, 1), (1, 2))
        for ((key, vertices) <- groupedVertices) {
          val SpatialKey(col, row) = key
          val correctVertices = base.map(_ + (row * 18 + col * 6))

          vertices
            .sortWith(_ < _)
            .zip(correctVertices)
            .foreach { case(v1, v2) =>
            v1 should be (v2)
          }

          coordinatesSet = coordinatesSet - ((row, col))
        }

        coordinatesSet.size should be (0)
      }

    }

  }
}
