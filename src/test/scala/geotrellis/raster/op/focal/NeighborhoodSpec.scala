package geotrellis.raster.op.focal

import geotrellis._

import scala.collection.mutable.Set
import scala.math._

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class NeighborhoodSpec extends FunSpec with ShouldMatchers {
  describe("Circle") {
    it("should mask all values outside the radius of a 7x7 circle neighborhood") {
      val expectedMaskedValues = Set((0,0),(0,1),(1,0),(0,6),(1,6),(0,5),(6,0),(6,1),(0,5),(6,6),(6,5),(5,6),
				     (2,6), (0,2), (6,4), (0,4), (4,6), (5,0), (2,0), (4,0), (6,2))
      val circle = Circle2(3)
      val result = Set[(Int,Int)]()
      for(x <- 0 to 6) {
	for(y <- 0 to 6) {
	  if(circle.mask(x,y)) { result += ((x,y)) }
	}
      }

      result should equal (expectedMaskedValues)
    }
  }
}
