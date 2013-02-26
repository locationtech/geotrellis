package geotrellis.io

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class LoadFileSpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer {
  describe("LoadFile") {
    it("loads a test raster.") {
      val raster = run(LoadFile("src/test/resources/fake.img8.arg"))

      raster.get(0, 0) should be (49)
      raster.get(3, 3) should be (4)
    }
  }
}
