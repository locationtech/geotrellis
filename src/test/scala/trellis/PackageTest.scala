package trellis

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConstantsTest extends FunSuite {
  test("constants") {
    assert(trellis.constant.NODATA === Int.MinValue)
  }
}
