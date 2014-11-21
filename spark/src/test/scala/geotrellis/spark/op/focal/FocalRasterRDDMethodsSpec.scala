package geotrellis.spark.op.focal

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.testfiles._

import geotrellis.raster._

import org.scalatest.FunSpec

class FocalRasterRDDMethodsSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark
    with RasterRDDBuilders {

  describe("Focal RasterRDD zipWithNeighbors") {
    ifCanRunSpark {

      it("should get correct neighbors for 3 x 2 tiles") {
        val rasterRDD = createRasterRDD(
          sc,
          ArrayTile(Array(
            1,  1,  1,   2,  2,  2,   3,  3,  3,
            1,  1,  1,   2,  2,  2,   3,  3,  3,

            4,  4,  4,   5,  5,  5,   6,  6,  6,
            4,  4,  4,   5,  5,  5,   6,  6,  6,

            7,  7,  7,   8,  8,  8,   9,  9,  9,
            7,  7,  7,   8,  8,  8,   9,  9,  9,

            10, 10, 10,  11, 11, 11,  12, 12, 12,
            10, 10, 10,  11, 11, 11,  12, 12, 12
          ), 9, 8),
          3, 2, 3, 4
        )

        val res = rasterRDD.zipWithNeighbors
          .map { case(k, t, n) => (k, t, n.getNeighbors) }.collect

        for((key, tile, neighbors) <- res) {
          tile.get(0, 0) match {
            case 1 => withClue("NW") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(2, 4, 5))
            }
            case 2 => withClue("N") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(1, 3, 4, 5, 6))
            }
            case 3 => withClue("NE") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(2, 5, 6))
            }
            case 4 => withClue("W") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(1, 2, 5, 7, 8))
            }
            case 5 => withClue("CENTER") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(1, 2, 3, 4, 6, 7, 8, 9))
            }
            case 6 => withClue("E") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(2, 3, 5, 8, 9))
            }
            case 7 => withClue("SW") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(4, 5, 8, 10, 11))
            }
            case 8 => withClue("S") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(4, 5, 6, 7, 9, 10, 11, 12))
            }
            case 9 => withClue("SE") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(5, 6, 8, 11, 12))
            }
            case 10 => withClue("LASTROW West") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(7, 8, 11))
            }
            case 11 => withClue("LASTROW Center") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(7, 8, 9, 10, 12))
            }
            case 12 => withClue("LASTROW East") {
              neighbors.flatten.map(_.get(0, 0))
                .sorted should be (Seq(8, 9, 11))
            }
            case _ => fail("Should never happen.")
          }
        }
      }
    }
  }
}
