package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._
import geotrellis.vector.Extent

import org.scalatest._
import geotrellis.testkit._

class AddSpec extends FunSpec
  with Matchers
  with TestEngine {

  describe("Add MultiBandTile") {
    val array1 = Array(0, -1, 2, -3,
      4, -5, 6, -7,
      8, -9, 10, -11,
      12, -13, 14, -15)

    val array2 = Array(0, 4, -5, 6,
      -1, 2, -3, -7,
      12, -13, 14, -15,
      8, -9, 10, -11)

    val array3 = Array(0, 0, 2, -3,
      4, -5, 6, -7,
      8, 0, 10, 0,
      12, -13, 14, -15)

    val array4 = Array(10, -1, 2, -3,
      4, -5, 6, -7,
      8, -9, 10, -11,
      12, -13, 14, -15)

    val tile1 = IntArrayTile(array1, 4, 4)
    val tile2 = IntArrayTile(array2, 4, 4)
    val tile3 = IntArrayTile(array3, 4, 4)
    val tile4 = IntArrayTile(array4, 4, 4)

    it("add constant") {
      val m: MultiBandTile = MultiBandTile(Array(tile1, tile2, tile3, tile4))
      val result = m + 10
      for (band <- 0 until m.bands) {
        for (col <- 0 until m.cols) {
          for (row <- 0 until m.rows) {
            result.getBand(band).get(col, row) should be(m.getBand(band).get(col, row) + 10)
          }
        }
      }
    }
  }
}