package geotrellis.raster

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class TileLayoutSpec extends FunSpec with ShouldMatchers {
  describe("TileLayout.fromTileDimensions") {
    it("should map an inverse function correctly.") {
      val extent = Extent(0,0,1000,1000)
      val re = RasterExtent(extent,10, 1, 100, 1000)
      val TileLayout(tileCols,tileRows,pixelCols,pixelRows) = TileLayout.fromTileDimensions(re,40,600)
      tileCols should be (3)
      tileRows should be (2)
      pixelCols should be (40)
      pixelRows should be (600)
    }
  }
}
