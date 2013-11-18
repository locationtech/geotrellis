package geotrellis.raster

import org.scalatest.FunSpec
import org.scalatest.matchers._

class BitArrayRasterDataSpec extends FunSpec with ShouldMatchers {
  describe("BitArrayRasterData.map") {
    it("should not destroy the compiler.") {
      val arr = Array[Byte](0,1,2,3,4,5,6,7,8)
      val b = BitArrayRasterData(arr,3,3)
      b.map(_+1)
    }
  }
}
