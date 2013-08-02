package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CombinationSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Combination") {
    it("gives correct number of combinations for simple case") {
      val r1 = 
        createRaster(Array(
          1, 2, 3, 4,
          1, 2, 3, 4,
          1, 2, 3, 4), 4, 3)

      val r2 = 
        createRaster(Array(
          4, 1, 2, 3,
          4, 1, 2, 3,
          4, 1, 2, 3), 4, 3)

      val r3 = 
        createRaster(Array(
          3, 4, 1, 2,
          3, 4, 1, 2,
          3, 4, 1, 2), 4, 3)

      run(Combination(r1,r2,r3)).findMinMax._2 should be (3)
    }

    it("gives correct number of combinations for a case where all combos are unique") {
      val r1 = 
        createRaster(Array(
          1, 2, 3, 4,
          2, 3, 4, 1,
          3, 4, 1, 2), 4, 3)

      val r2 = 
        createRaster(Array(
          4, 1, 2, 3,
          3, 4, 1, 2,
          2, 3, 1, 4), 4, 3)

      val r3 = 
        createRaster(Array(
          3, 4, 2, 1,
          2, 3, 1, 4,
          4, 3, 2, 1), 4, 3)

      run(Combination(r1,r2,r3)).findMinMax._2 should be (4*3 - 1)
    }
  }
}
