package geotrellis.feature.spec

import geotrellis.feature._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest.FunSpec
import org.scalatest.matchers._

class PolygonSpec extends FunSpec with ShouldMatchers {
  describe("Polygon") {
    it("should be a closed Polygon if constructed with l(0) == l(-1)") {
      val p = Polygon(Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0))))
      p.exterior.isClosed should be (true)
    }
    
    it("should throw if attempt to construct with unclosed line") {
      intercept[Exception] {
        val p = Polygon(Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1))))
      }
    }
  }
}
