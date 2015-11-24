package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.testkit._

import org.scalatest._

class RenderMethodsSpec extends FunSpec with Matchers 
                                        with TestEngine 
                                        with TileBuilders {
  describe("color") {
    it("should color an int tile") {
      val arr = (0 to 120).map { z => ((z.toDouble / 120) * 100).toInt }.toArray
      val tile = createTile(arr, 10, 12)

      val limits = Array(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
      val colors = Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      val colorBreaks = ColorBreaks(limits, colors)

      val result = tile.color(colorBreaks, LessThanOrEqualTo)

      result.foreachDouble { (col, row, z) =>
        val i = tile.cols * row + col
        val expected = ((i.toDouble / 120) * 10).toInt
        z should be (expected)
      }
    }

    it("should color a double tile") {
      val arr = (0 to 120).map(_.toDouble / 120).toArray
      val tile = createTile(arr, 10, 12)

      val limits = Array(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)
      val colors = Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      val colorBreaks = ColorBreaks(limits, colors)

      val result = tile.color(colorBreaks, LessThanOrEqualTo)

      result.foreachDouble { (col, row, z) =>
        val i = tile.cols * row + col
        val expected = ((i.toDouble / 120) * 10).toInt
        z should be (expected)
      }
    }
  }
}
