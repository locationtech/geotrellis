package geotrellis.raster

import org.scalatest._

class TileLayoutSpec extends FunSpec with Matchers {
  describe("TileLayout"){
    it("converts from tile index to tile XY"){
      val layout = TileLayout(3,2,10,10)
      val col = 1
      val row = 2
      val index = layout.getTileIndex(col, row)
      val (outCol, outRow) = layout.getTileXY(index)

      outCol should equal (col)
      outRow should equal (row)
    }
  }

}
