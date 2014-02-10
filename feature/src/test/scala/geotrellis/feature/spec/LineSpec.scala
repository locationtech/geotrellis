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
  }
}
