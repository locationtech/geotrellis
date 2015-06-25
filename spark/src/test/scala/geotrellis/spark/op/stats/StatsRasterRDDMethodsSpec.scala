package geotrellis.spark.op.stats

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._

import geotrellis.raster._
import geotrellis.spark.testkit._
import geotrellis.vector._

import org.scalatest.FunSpec

import collection._

class StatsRasterRDDMethodsSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("RDD Stats Method Operations") {

    ifCanRunSpark {

      it("gives correct class breaks for example raster histogram") {
        val rdd = createRasterRDD(
          sc,
          ArrayTile(Array(
            1, 1, 1,  1, 1, 1,  1, 1, 1,
            1, 1, 1,  1, 1, 1,  1, 1, 1,

            2, 2, 2,  2, 2, 2,  2, 2, 2,
            2, 2, 2,  2, 2, 2,  2, 2, 2,

            3, 3, 3,  3, 3, 3,  3, 3, 3,
            3, 3, 3,  3, 3, 3,  3, 3, 3,

            4, 4, 4,  4, 4, 4,  4, 4, 4,
            4, 4, 4,  4, 4, 4,  4, 4, 4), 9, 8),
          TileLayout(3, 4, 3, 2)
        )

        val classBreaks = rdd.classBreaks(3)

        classBreaks should be (Array(1, 3, 4))
      }
    }
  }
}
