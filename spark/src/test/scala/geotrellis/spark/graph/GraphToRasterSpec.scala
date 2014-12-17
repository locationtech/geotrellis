package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.raster._

import org.scalatest.FunSpec

class GraphToRasterSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Graph to Raster Spec") {

    def assertCorrectGraphToRaster(rasterRDD: RasterRDD[SpatialKey]) = {
      val resultRDD = rasterRDD.toGraph.toRaster

      val resultArray = resultRDD.stitch.toArray
      val correctArray = rasterRDD.stitch.toArray

      resultArray should be (correctArray)

      resultRDD.metaData should be (rasterRDD.metaData)
    }

    ifCanRunSpark {

      val nd = NODATA

      it("should successfully convert a graph to the correct raster #1") {
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

        assertCorrectGraphToRaster(rasterRDD)
      }

      it("should successfully convert a graph to the correct raster #2") {
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

        assertCorrectGraphToRaster(rasterRDD)
      }

    }

  }
}
