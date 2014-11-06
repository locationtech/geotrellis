package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import collection._

import org.scalatest._

/**
  * Since the abstract bicubic interpolation inherits from cubic
  * interpolation all that is tested here is that the points are evaluated
  * and passed to the uniCubicInterpolation method in the correct order.
  */
class BicubicInterpolationSpec extends FunSpec with Matchers {

  // Returned if bicubic interpolation is used.
  // Bilinear interpolation should never be able to return this
  // value from the given tile and extent.
  val B = -1337

  class BicubicInterpolation4By4(tile: Tile, extent: Extent) extends
      BicubicInterpolation(tile, extent, 4) {

    override def uniCubicInterpolation(p: Array[Double], x: Double): Double = B

  }

  class BicubicInterpolation6By6(tile: Tile, extent: Extent) extends
      BicubicInterpolation(tile, extent, 6) {

    override def uniCubicInterpolation(p: Array[Double], x: Double): Double = B

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

      val lastInterpArr = Array.ofDim[Double](d)
      for (i <- 0 until d) lastInterpArr(i) = (d - i)

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

        val interp = new BicubicInterpolation(tile, extent, d) {
          override def uniCubicInterpolation(
            p: Array[Double],
            x: Double): Double = {
            if (q.isEmpty && c != 1) fail
            else if (!q.isEmpty) {
              val arr = q.dequeue
              p should be (arr)
            } else {
              p should be (lastInterpArr)
            }

            c -= 1

            c
          }
        }

        withClue(s"Failed on ($x, $y): ") {
          interp.interpolate(x, y) should be (c)
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
