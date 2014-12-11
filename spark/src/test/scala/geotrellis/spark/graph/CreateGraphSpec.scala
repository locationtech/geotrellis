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

        graph.numEdges should be (1337) // TODO: count this manually
      }

    }

  }
}
