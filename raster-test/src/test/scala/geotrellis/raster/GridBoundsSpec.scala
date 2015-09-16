package geotrellis.raster

import org.scalatest._
import spire.syntax.cfor._

class GridBoundsSpec extends FunSpec with Matchers{
  describe("GridBounds.minus") {
    it("subtracts an overlapping GridBounds that overflows bottom left") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(50, 50, 150, 150)
      val result = (minuend - subtrahend).sortBy(_.colMax).toArray

      result.size should be (2)

      // Account for the 2 possible cuts
      if(result(0).rowMin == 0) {
        result(0) should be (GridBounds(0, 0, 49, 100))
        result(1) should be (GridBounds(50, 0, 100, 49))
      } else {
        result(0) should be (GridBounds(0, 50, 49, 100))
        result(1) should be (GridBounds(0, 0, 100, 49))
      }
    }

    it("subtracts an overlapping GridBounds that overflows top right") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(-50, -50, 50, 50)
      val result = (minuend - subtrahend).sortBy(_.colMin).toArray

      println(result.toSeq)
      result.size should be (2)

      // Account for the 2 possible cuts
      if(result(0).colMax == 50) {
        result(0) should be (GridBounds(0, 51, 50, 100))
        result(1) should be (GridBounds(51, 0, 100, 100))
      } else {
        result(0) should be (GridBounds(0, 51, 100, 100))
        result(1) should be (GridBounds(51, 0, 100, 100))
      }
    }

    it("subtracts a partial horizontal line through the middle") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(-50, 50, 50, 50)
      val result = (minuend - subtrahend).sortBy(_.colMin).toSet

      result should be (Set(
        GridBounds(51, 0, 100, 100), // Right
        GridBounds(0, 0, 50, 49), // Top
        GridBounds(0, 51, 50, 100) // Bottom
      ))
    }

    it("subtracts a contained bounds") {
      val minuend = GridBounds(0, 0, 100, 100)
      val subtrahend = GridBounds(25, 35, 75, 85)
      val result = (minuend - subtrahend).sortBy(_.colMin).toSet

      result should be (Set(
        GridBounds(0, 0, 24, 100), // Left
        GridBounds(76, 0, 100, 100), // Right
        GridBounds(25, 0, 75, 34), // Top
        GridBounds(25, 86, 75, 100) // Bottom
      ))
    }

    it("subtracts full bounds") {
      val minuend = GridBounds(9,10,16,13)
      val subtrahend = GridBounds(8,9,17,14)

      (minuend - subtrahend) should be (Seq())
    }
  }

  describe("GridBounds.distinct") {
    it("creates a distinct set of GridBounds from an overlapping set") {
      val gridBounds =
        Seq(
          GridBounds(0, 0, 75, 75),
          GridBounds(25, 25, 100, 100)
        )
      println(GridBounds.distinct(gridBounds))
      GridBounds.distinct(gridBounds).map(_.size).sum should be ((101 * 101) - (25 * 25 * 2))
    }
  }
}
