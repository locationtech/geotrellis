package geotrellis.raster

import org.scalatest.FunSpec
import org.scalatest.matchers._

class BitArrayRasterDataSpec extends FunSpec with ShouldMatchers {
  describe("BitArrayRasterData.map") {
    it("should map an inverse function correctly.") {
      val arr = Array[Byte](0,1,2,3,4,5,6,7,8)
      val b = BitArrayRasterData(arr,3*8,3)
      val expected = BitArrayRasterData(arr.map(b => (~b).toByte),3*8,3).toArray
      b.map(i => i+1).toArray should be (expected)
    }
  }
}
 
