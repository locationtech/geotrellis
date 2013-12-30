package geotrellis.util

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class FilesystemSpec extends FunSpec 
                        with ShouldMatchers {
  describe("Filesystem") {
    it("should give the same array for slurp and mapToByteArray for whole array") {
      val path = "src/test/resources/fake.img32.json"
      val bytes1 = Filesystem.slurp(path)
      val bytes2 = Array.ofDim[Byte](bytes1.size)
      Filesystem.mapToByteArray(path,bytes2,0,bytes2.size)
      bytes1 should be (bytes2)
    }
  }
}

