package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import collection._

import org.scalatest._

/**
  * Since the abstract cubic resample inherits from bilinear
  * resample there is no need to actually test that the implementation
  * resolves the correct points given the extent coordinates when
  * bilinear resample takes over, since that is tested in the bilinear
  * resample testing specification.
  *
  * Instead, this specification tests that the implementation correctly falls
  * back to bilinear when D * D points surrounding the map coordinate point
  * can't be resolved. D is here the size of the side of the cube.
  * It also tests that it uses the cubic resample when it should, and that
  * the cubic resample resolves the correct D * D points.
  */
class CubicResampleSpec extends FunSpec with Matchers {

  // Returned if cubic resample is used.
  // Bilinear resample should never be able to return this
  // value from the given tile and extent.
  val B = -1337

  class CubicResample4By4(tile: Tile, extent: Extent) extends
      CubicResample(tile, extent, 4) {

    override def cubicResample(
      p: Tile,
      x: Double,
      y: Double): Double = B

  }

  class CubicResample6By6(tile: Tile, extent: Extent) extends
      CubicResample(tile, extent, 6) {

    override def cubicResample(
      p: Tile,
      x: Double,
      y: Double): Double = B

  }

  val Epsilon = 1e-9

  describe("it should use bicubic resample when D points can be resolved") {

    def whenToNotUseBilinear(
      int: Resample,
      leftStart: Int,
      rightEnd: Int,
      bottomStart: Int,
      topEnd: Int) = {
      for (i <- leftStart to rightEnd; j <- bottomStart to topEnd)
        withClue(s"Failed on ($i, $j): ") {
          int.resample(i / 10.0, j / 10.0) should be (B)
        }
    }

    it("should use bicubic resample when can resolve all 16 points") {
      val tile = ArrayTile(Array.fill[Int](10000)(100), 100, 100)
      val extent = Extent(0, 0, 100, 100)
      val ci = new CubicResample4By4(tile, extent)
      whenToNotUseBilinear(ci, 15, 984, 16, 985)
    }

    it("should use bicubic resample when can resolve all 36 points") {
      val tile = ArrayTile(Array.fill[Int](10000)(100), 100, 100)
      val extent = Extent(0, 0, 100, 100)
      val ci = new CubicResample6By6(tile, extent)
      whenToNotUseBilinear(ci, 25, 974, 26, 975)
    }

  }

  describe("it should use bilinear resample but only when when needed") {

    def whenToUseBilinear(
      int: Resample,
      leftEnd: Int,
      rightStart: Int,
      bottomEnd: Int,
      topStart: Int) = {
      val max = 1000
      for (i <- 0 to leftEnd; j <- 0 to max) // left
        withClue(s"Failed on ($i, $j): ") {
          int.resample(i / 10.0, j / 10.0) should be (100)
        }

      for (i <- rightStart to max; j <- 0 to max) // right
        withClue(s"Failed on ($i, $j): ") {
          int.resample(i / 10.0, j / 10.0) should be (100)
        }

      for (i <- 0 to max; j <- 0 to bottomEnd) // bottom
        withClue(s"Failed on ($i, $j): ") {
          int.resample(i / 10.0, j / 10.0) should be (100)
        }

      for (i <- 0 to max; j <- topStart to max) // top
        withClue(s"Failed on ($i, $j): ") {
          int.resample(i / 10.0, j / 10.0) should be (100)
        }
    }

    it("should use bilinear resample when 4 * 4 points can't be resolved") {
      val tile = ArrayTile(Array.fill[Int](10000)(100), 100, 100)
      val extent = Extent(0, 0, 100, 100)
      val ci = new CubicResample4By4(tile, extent)
      whenToUseBilinear(ci, 14, 985, 15, 986)
    }

    it("should use bilinear resample when 6 * 6 points can't be resolved") {
      val tile = ArrayTile(Array.fill[Int](10000)(100), 100, 100)
      val extent = Extent(0, 0, 100, 100)
      val ci = new CubicResample6By6(tile, extent)
      whenToUseBilinear(ci, 24, 975, 25, 976)
    }

  }

  describe("it should resolve the correct D * D points") {

    def resolvesCorrectDByDPoints(d: Int) = {
      val d2 = d * d
      val tileArray = (for (i <- 0 until d2) yield
        (List.range(i * d2, (i + 1) * d2).toArray)).flatten.toArray

      val tile = ArrayTile(tileArray, d2, d2)
      val extent = Extent(0, 0, d2, d2)

      val cellSize = 0.5

      val h = d / 2

      val lastResampArr = Array.ofDim[Double](d)
      for (i <- 0 until d) lastResampArr(i) = (d - i)

      for (i <- h - 1 until d2 - h; j <- d2 - h until h - 1 by -1) {
        val (x, y) = (cellSize + i, cellSize + j)

        val t = ArrayTile(Array.ofDim[Double](d * d), d, d)
        for (k <- 0 until d; l <- 0 until d)
          t.setDouble(l, k, i - (h - 1) + l + (k + d2 - h - j) * d2)

        val resamp = new CubicResample(tile, extent, d) {
          override def cubicResample(
            p: Tile,
            x: Double,
            y: Double): Double = {
            p.toArray should be(t.toArray)
            B
          }
        }

        withClue(s"Failed on ($x, $y): ") {
          resamp.resample(x, y) should be (B)
        }
      }
    }

    it("should resolve the correct 16 points") {
      resolvesCorrectDByDPoints(4)
    }

    it("should resolve the correct 36 points") {
      resolvesCorrectDByDPoints(6)
    }

  }

}
