package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import collection._

import org.scalatest._

/**
  * Since the abstract bicubic resample inherits from cubic
  * resample all that is tested here is that the points are evaluated
  * and passed to the uniCubicResample method in the correct order.
  */
class BicubicResampleSpec extends FunSpec with Matchers {

  // Returned if bicubic resample is used.
  // Bilinear resample should never be able to return this
  // value from the given tile and extent.
  val B = -1337

  class BicubicResample4By4(tile: Tile, extent: Extent) extends
      BicubicResample(tile, extent, 4) {

    override def uniCubicResample(p: Array[Double], x: Double): Double = B

  }

  class BicubicResample6By6(tile: Tile, extent: Extent) extends
      BicubicResample(tile, extent, 6) {

    override def uniCubicResample(p: Array[Double], x: Double): Double = B

  }

  val Epsilon = 1e-9

  describe("it should resolve the correct D * D points in the correct order") {

    def resolvesCorrectDByDPointsInCorrectOrder(d: Int) = {
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

        val q = mutable.Queue[Array[Double]]()
        for (k <- 0 until d) {
          val arr = Array.ofDim[Double](d)
          q += arr
          for (l <- 0 until d)
            arr(l) = i - (h - 1) + l + (k + d2 - h - j) * d2
        }

        var c = q.size + 1

        val resamp = new BicubicResample(tile, extent, d) {
          override def uniCubicResample(
            p: Array[Double],
            x: Double): Double = {
            if (q.isEmpty && c != 1) fail
            else if (!q.isEmpty) {
              val arr = q.dequeue
              p should be (arr)
            } else {
              p should be (lastResampArr)
            }

            c -= 1

            c
          }
        }

        withClue(s"Failed on ($x, $y): ") {
          resamp.resample(x, y) should be (c)
        }

        c should be (0)
        q.size should be (0)
      }
    }

    it("should resolve the correct 16 points in correct order") {
      resolvesCorrectDByDPointsInCorrectOrder(4)
    }

    it("should resolve the correct 36 points in correct order") {
      resolvesCorrectDByDPointsInCorrectOrder(6)
    }

  }

}
