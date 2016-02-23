package geotrellis.spark.io.index.zcurve

import org.scalatest._

class Z3Spec extends FunSpec with Matchers {
  describe("Z3 encoding") {
    it("interlaces bits"){
      // (x,y,z) - x has the lowest sigfig bit
      Z3(1,0,0).z should equal(1)
      Z3(0,1,0).z should equal(2)      
      Z3(0,0,1).z should equal(4)
      Z3(1,1,1).z should equal(7)
    }

    it("deinterlaces bits") {
      Z3(23,13,200).decode  should equal(23, 13, 200)
      
      //only 21 bits are saved, so Int.MaxValue is CHOPPED
      Z3(Int.MaxValue, 0, 0).decode should equal(2097151, 0, 0)
      Z3(Int.MaxValue, 0, Int.MaxValue).decode should equal(2097151, 0, 2097151)
    }

    it("unapply"){
      val Z3(x,y,z) = Z3(3,5,1)
      x should be (3)
      y should be (5)
      z should be (1)
    }    
  }
}
