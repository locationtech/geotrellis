package geotrellis.spark.graphx

import geotrellis.spark._
import geotrellis.spark.graphx._

import geotrellis.raster._

import org.scalatest.FunSpec

class GraphToRasterSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Graph to Raster Spec") {

    ifCanRunSpark {

      val nd = NODATA

      it("should successfully convert a graph to the correct raster") {
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

        val resultRDD = rasterRDD.toGraph.toRaster

        val resultArray = resultRDD.stitch.toArray
        val correctArray = rasterRDD.stitch.toArray

        resultArray should be (correctArray)

        resultRDD.metaData should be (rasterRDD.metaData)
      }

    }

  }
}
