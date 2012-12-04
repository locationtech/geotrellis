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
      val circle = Circle(3)
      val result = Set[(Int,Int)]()
      for(x <- 0 to 6) {
	for(y <- 0 to 6) {
	  if(circle.mask(x,y)) { result += ((x,y)) }
	}
      }

      result should equal (expectedMaskedValues)
    }
  }

  describe("Wedge") {
    it("should match a picture of a wedge pointing right") {
      val mask = TestCursor.maskFuncFromString("""
                 X  X  X  X  X  X  X  X  X  X  X  X  X
                 X  X  X  X  X  X  X  X  X  X  X  X  X
                 X  X  X  X  X  X  X  X  X  X  0  X  X
                 X  X  X  X  X  X  X  X  X  0  0  0  X
                 X  X  X  X  X  X  X  X  0  0  0  0  X
                 X  X  X  X  X  X  X  0  0  0  0  0  X
                 X  X  X  X  X  X  0  0  0  0  0  0  0
                 X  X  X  X  X  X  X  0  0  0  0  0  X
                 X  X  X  X  X  X  X  X  0  0  0  0  X
                 X  X  X  X  X  X  X  X  X  0  0  0  X
                 X  X  X  X  X  X  X  X  X  X  0  X  X
                 X  X  X  X  X  X  X  X  X  X  X  X  X
                 X  X  X  X  X  X  X  X  X  X  X  X  X
                                               """)
      
      val expected = Set[(Int,Int)]()
      val actual = Set[(Int,Int)]()

      val wedge = new Wedge(6, 315, 45)
      for(x <- 0 to 12) {
        for(y <- 0 to 12) {
          if(!mask(x,y)) { expected += ((x,y)) }
          if(!wedge.mask(x,y)) { actual += ((x,y)) }
        }
      }

      actual should equal (expected)
    }

    it("should match a picture of a wedge pointing up") {
      val mask = TestCursor.maskFuncFromString("""
                 X X X X X X 0 X X X X X X
                 X X X 0 0 0 0 0 0 0 X X X
                 X X 0 0 0 0 0 0 0 0 0 X X
                 X X X 0 0 0 0 0 0 0 X X X
                 X X X X 0 0 0 0 0 X X X X
                 X X X X X 0 0 0 X X X X X
                 X X X X X X 0 X X X X X X
                 X X X X X X X X X X X X X
                 X X X X X X X X X X X X X
                 X X X X X X X X X X X X X
                 X X X X X X X X X X X X X
                 X X X X X X X X X X X X X
                 X X X X X X X X X X X X X
                                               """)
      
      val expected = Set[(Int,Int)]()
      val actual = Set[(Int,Int)]()

      val wedge = new Wedge(6, 45, 135)
      for(x <- 0 to 12) {
        for(y <- 0 to 12) {
          if(!mask(x,y)) { expected += ((x,y)) }
          if(!wedge.mask(x,y)) { actual += ((x,y)) }
        }
      }

      actual should equal (expected)
    }
  }

  describe("Annulus") {
    it("should match a picture of homer simpson's dream") { 
      val mask = TestCursor.maskFuncFromString("""
                 X X X X X X 0 X X X X X X
                 X X X 0 0 0 0 0 0 0 X X X
                 X X 0 0 0 0 0 0 0 0 0 X X
                 X 0 0 0 X X X X X 0 0 0 X
                 X 0 0 X X X X X X X 0 0 X
                 X 0 0 X X X X X X X 0 0 X
                 0 0 0 X X X X X X X 0 0 0
                 X 0 0 X X X X X X X 0 0 X
                 X 0 0 X X X X X X X 0 0 X
                 X 0 0 0 X X X X X 0 0 0 X
                 X X 0 0 0 0 0 0 0 0 0 X X
                 X X X 0 0 0 0 0 0 0 X X X
                 X X X X X X 0 X X X X X X
                                               """)
      
      val expected = Set[(Int,Int)]()
      val actual = Set[(Int,Int)]()

      val annulus = new Annulus(4,6)

      for(x <- 0 to 12) {
        for(y <- 0 to 12) {
          if(!mask(x,y)) { expected += ((x,y)) }
          if(!annulus.mask(x,y)) { actual += ((x,y)) }
        }
      }

      println((actual -- expected).toString)

      actual should equal (expected)
    }
  }
}
