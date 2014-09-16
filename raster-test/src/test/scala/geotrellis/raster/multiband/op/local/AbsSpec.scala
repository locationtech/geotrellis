package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

import org.scalatest._

import geotrellis.testkit._

class AbsSpec extends FunSpec
  with Matchers
  with TestEngine
  with MultiBandTileBuilder {

  describe("Abs MultiBandTile") {
    it("takes the absolute value of each cell of an int multiband raster") {

      val intMB = absIntmb
      val result = intMB.localAbs

      for (b <- 0 until intMB.bands) {
        for (c <- 0 until intMB.cols) {
          for (r <- 0 until intMB.rows) {
            if (isNoData(intMB.getBand(b).get(c, r)))
              result.getBand(b).get(c, r) should be(NODATA)
            else
              result.getBand(b).get(c, r) should be(b + 1)
          }
        }
      }
    }
    
    it("takes the absolute value of each cell of a double multiband raster") {

      val dbMB = absDubmb 
      val result = dbMB.localAbs

      for (b <- 0 until dbMB.bands) {
        for (c <- 0 until dbMB.cols) {
          for (r <- 0 until dbMB.rows) {
            if ((dbMB.getBand(b).getDouble(c, r)).isNaN())
              result.getBand(b).getDouble(c, r).isNaN should be(true)
            else
              result.getBand(b).getDouble(c, r) should be((b + 1).toDouble)
          }
        }
      }
    }

  }

}