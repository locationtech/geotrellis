package geotrellis.util

import org.scalatest._

class ComponentSpec extends FunSpec with Matchers {
  class Upper
  class Middle extends Upper
  class Lower extends Middle

  describe("Identity components") {
    it("should get and set identity") {
      val m = new Middle
      val m2 = new Middle
      m.getComponent[Middle] should be (m)
      m.setComponent[Middle](m2) should be (m2)
    }

    it("should get identity that is subtype") {
      val m = new Middle
      m.getComponent[Upper] should be (m)
    }

    it("should set identity that is supertype") {
      val m = new Middle
      val m2 = new Lower
      m.setComponent[Middle](m2) should be (m2)
    }
  }
}
