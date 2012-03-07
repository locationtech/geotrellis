package geotrellis.extent

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import geotrellis.Extent;

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ExtentSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("An Extent object") {
    it("should die when invalid #1") {
      evaluating {
        Extent(10.0, 10.0, 0.0, 20.0)
      } should produce [Exception];
    }
  }
}
