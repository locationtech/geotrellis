package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.raster._

import org.scalatest.FunSpec

class GraphConversionSpec extends FunSpec with TestEnvironment
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Graph Conversion Spec") {

    def assertCorrectGraphConversion(rasterRDD: RasterRDD[SpatialKey]) = {
      val resultRDD = rasterRDD.toGraph.toRaster

      val resultArray = resultRDD.stitch.toArray
      val correctArray = rasterRDD.stitch.toArray

      resultArray should be (correctArray)

      resultRDD.metaData should be (rasterRDD.metaData)
    }

    ifCanRunSpark {

      val nd = NODATA

      it("should successfully convert a raster to a graph and back #1") {
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

        assertCorrectGraphConversion(rasterRDD)
      }

      it("should successfully convert a raster to a graph and back #2") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            nd, nd, nd,   1, 1, 1,   1, 1, 1,
            nd, nd, nd,   2, 1, 2,   1, 3, 1,

            3,  8,  1,    3, 3, 3,   nd, nd, nd,
            2,  1,  7,    1, nd,1,   nd, nd, nd
          ), 9, 4),
          TileLayout(3, 2, 3, 2)
        )

        assertCorrectGraphConversion(rasterRDD)
      }

    }

  }
}
