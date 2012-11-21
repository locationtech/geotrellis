package geotrellis.raster.op.focal

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis._
import geotrellis.raster._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class FocalStrategySpec extends FunSpec with ShouldMatchers {
  describe("FocalStrategy") {
    it("should visit every cell") {
      val rex = RasterExtent(Extent(0,0,5,5), 1,1,5,5)
      val d = Array(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25)
      val r = Raster(d,rex)
      val b = IntRasterBuilder(rex)
      val result = IntFocalStrategy.execute(r,b) { nh => -1 }
      result.foreach { i =>
    	i should be (-1)
      }
    }

    it("should calculate sum") {
      val rex = RasterExtent(Extent(0,0,5,5), 1,1,5,5)
      val d = Array(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
      val r = Raster(d,rex)
      val b = IntRasterBuilder(rex)
      val result = IntFocalStrategy.execute(r,b){ cur => 
	val x = cur.foldLeft(0) { (a,b) => a + b }
	x 
      }

      // Check the corners
      result.get(0,0) should be (4)
      result.get(4,0) should be (4)
      result.get(0,4) should be (4)
      result.get(4,4) should be (4)

      // Check edges
      result.get(2,0) should be (6)
      result.get(4,2) should be (6)
      result.get(2,4) should be (6)
      result.get(0,2) should be (6)

      // Check the center
      result.get(2,2) should be (9)
    }
  
    it("should calculate subtraction") {
      val rex = RasterExtent(Extent(0,0,5,5), 1,1,5,5)
      val d = Array(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25)
      val r = Raster(d,rex)
      val b = IntRasterBuilder(rex)
      val result = IntFocalStrategy.execute(r,b) { cur => cur.foldLeft(0) { (a,b) => a - b } }
      
      // At (1,1), should be 0 -1 -2 -3 -6 -7 -8 -11 -12 -13
      result.get(1,1) should be (-63)
    }
  }
}
