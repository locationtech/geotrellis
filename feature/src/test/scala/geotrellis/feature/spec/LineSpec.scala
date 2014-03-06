package geotrellis.feature.spec

import geotrellis.feature._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest.FunSpec
import org.scalatest.matchers._

class LineSpec extends FunSpec with ShouldMatchers {
  describe("Line") {
    it("should be a closed Line if constructed with l(0) == l(-1)") {
      val l = Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0)))
      l.isClosed should be (true)
    }

    it("should return true for crosses for MultiLine it crosses") {
      val l = Line( (0.0, 0.0), (5.0, 5.0) )
      val ml = 
        MultiLine (
          Line( (1.0, 0.0), (1.0, 5.0) ),
          Line( (2.0, 0.0), (2.0, 5.0) ),
          Line( (3.0, 0.0), (3.0, 5.0) ),
          Line( (4.0, 0.0), (4.0, 5.0) )
        )

      l.crosses(ml) should be (true)
    }
  }
}
