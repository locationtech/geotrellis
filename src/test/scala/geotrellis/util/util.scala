package geotrellis.util

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class UtilSpec extends Spec with MustMatchers {
  describe("Util package") {
    it("implements time()") {
      val (n, t) = Timer.time { Thread.sleep(100); 99 }
      n must be === 99
      t >= 100 must be === true
    }

    it("implements run()") {
      val n = Timer.run { 99 }
      n must be === 99
    }

    it("implements log()") {
      val i = 3
      val n = Timer.log("msg(%d)", i) { 99 }
      n must be === 99
    }
  }
}
