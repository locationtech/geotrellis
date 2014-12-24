package geotrellis.raster.op.local

import geotrellis.raster._
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._

class VarianceSpec extends FunSpec
    with Matchers
    with TestEngine
    with TileBuilders {

  describe("Variance") {

    it("should correctly compute the variance of an int raster") {
      val (cols, rows) = (3, 3)
      val r1 = createTile(Array(
        1, 2, 3,
        4, 5, 6,
        7, 8, NODATA
      ), rows, cols)

      val r2 = createTile(Array(
        41, 2, 5,
        4, NODATA, 6,
        12, 8, NODATA
      ), rows, cols)

      val r3 = createTile(Array(
        9, 1, 3,
        4, 7, 6,
        7, 8, 133700
      ), rows, cols)

      val r4 = createTile(Array(
        5, 0, 30,
        4, 3, 2,
        123, 7, 133800
      ), rows, cols)

      val answers = Seq(
        335,
        1,
        174,
        0,
        4,
        4,
        3274,
        0, // 0.1875
        5000
      )

      val seq = Seq(r1, r2, r3, r4)

      val res = seq.localVariance

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          withClue(s"Failed on row: $row, col: $col") {
            res.get(col, row) should be(answers(row * 3 + col))
          }
        }
      }
    }

    val Eps = 1e-5

    it("should correctly compute the variance of an double raster") {
      val (cols, rows) = (3, 3)
      val r1 = createTile(Array(
        1.5, 2.3, 3.7,
        4.21, 5.3, 6.1,
        7.1, 8.2, Double.NaN
      ), rows, cols)

      val r2 = createTile(Array(
        41.5, 2.1, 5.8,
        4.1, Double.NaN, 6.1,
        12.7, 8.2, Double.NaN
      ), rows, cols)

      val r3 = createTile(Array(
        9.13, 0.99, 3.2,
        4.222, 7.12, 6.4,
        7.1, 8.7, 133700
      ), rows, cols)

      val r4 = createTile(Array(
        5.6, 0.1, 30.2,
        4.1, 3.123, 2.11,
        123.1, 7.1, 133800.1337
      ), rows, cols)

      val answers = Seq(
        335.34289166667,
        1.0516916666667,
        169.83583333333,
        0.0045093333333313,
        4.004623,
        4.202025,
        3263.5733333333,
        0.45666666666665,
        5013.3789367676
      )

      val seq = Seq(r1, r2, r3, r4)

      val res = seq.localVariance

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          withClue(s"Failed on row: $row, col: $col") {
            res.getDouble(col, row) should be(answers(row * 3 + col) +- Eps)
          }
        }
      }
    }

  }

}
