package geotrellis.spark.io.index.zcurve 

import org.scalatest._

class Z3RangeSpec extends FunSpec with Matchers {

  describe("Z3Range") {
    it("list some elemnts") {
      val (x1, y1, z1) = (2,3,0)
      val (x2, y2, z2) = (10, 10,2000)

      val min = Z3(x1,y1, z1)
      val max = Z3(x2,y2, z2)
      val ranges = Z3.zranges(min, max)
      var actualSet: Set[(Long, Long, Long)] = Set.empty
      var count: Int = 0

      ranges foreach { case (min, max) =>
        for (z <- min to max) {
          val zobj = new Z3(z)
          actualSet =  actualSet + Tuple3(zobj.dim(0),zobj.dim(1),zobj.dim(2))
          count += 1
        }
      }

      var expectedSet: Set[(Long, Long, Long)] = Set.empty
      for  {
        z <- z1 to z2
        y <- y1 to y2
        x <- x1 to x2
      } {        
        expectedSet = expectedSet + Tuple3(x,y,z)
      }

      expectedSet should equal (actualSet)
      expectedSet.size should equal (count)
      expectedSet.size should equal ((z2-z1+1)*(y2-y1+1)*(x2-x1+1))
    }

    it("must handle this case") {
      val last = Z3(8,8,2040)
      val range = Z3Range(Z3(4,12,2044), Z3(5,5,2054))
      require(last.z > range.min.z)
      require(last.z < range.max.z)

      val (litmax, bigmin)  = range.zdivide(last)

      bigmin.z should not be (0L)
    }

  }
}