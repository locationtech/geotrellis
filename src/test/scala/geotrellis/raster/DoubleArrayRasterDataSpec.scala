package geotrellis.raster

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class DoubleArrayRasterDataSpec extends FunSpec 
                                  with ShouldMatchers 
                                  with TestServer 
                                  with RasterBuilders {
  describe("DoubleArrayRasterData.toByteArray") {
    it("converts back and forth.") {
      val data = probabilityRaster.asInstanceOf[ArrayRaster].data
      val (cols,rows) = (data.cols,data.rows)
      val data2 = DoubleArrayRasterData.fromArrayByte(data.toArrayByte,cols,rows)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          withClue(s"Values different at ($col,$row)") {
            data2.getDouble(col,row) should be (data.getDouble(col,row))
          }
        }
      }
    }
  }
}
