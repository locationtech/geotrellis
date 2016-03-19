package geotrellis.raster.prototype

import geotrellis.raster._
import geotrellis.raster.testkit._

import org.scalatest._

class TilePrototypeMethodsSpec extends FunSpec
    with Matchers
    with TileBuilders
    with RasterMatchers {
  describe("SinglebandTileMergeMethods") {
    it("should prototype correctly for NoNoData cell types") {
      val cellTypes: Seq[CellType] =
        Seq(
          ByteCellType,
          UByteCellType,
          ShortCellType,
          UShortCellType,
          IntCellType,
          FloatCellType,
          DoubleCellType
        )

      for(ct <- cellTypes) {
        val arr = Array(0.0, 1.0, 1.0, Double.NaN)
        val tile = DoubleArrayTile(arr, 2, 2, DoubleCellType).convert(ct)
        withClue(s"Failed for cell type $ct") {
          assertEqual(tile.prototype(ct, 2, 2), ArrayTile.alloc(ct, 2, 2))
        }
      }
    }
  }
}
