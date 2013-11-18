package geotrellis

import org.scalatest.FunSuite
import org.scalatest.matchers._

class PackageTest extends FunSuite with ShouldMatchers {
  test("constants") {
    assert(NODATA === Int.MinValue)
  }

  test("isNoData - Int") {
    val v = Int.MinValue + 1
    val y = 123

    isNoData(v - 1) should be (true)
    isNoData(y) should be (false)
  }

  test("isData - Int") {
    val v = Int.MinValue + 1
    val y = 123

    isData(v-1) should be (false)
    isData(y) should be (true)
  }

  test("isNoData - Double") {
    val x = 2.1 + Double.NaN
    val y = 1.3

    isNoData(x*3 - 1.0) should be (true)
    isNoData(y) should be (false)
  }

  test("isData - Double") {
    val x = 2.1 + Double.NaN
    val y = 1.3

    isData(x*3 - 1.0) should be (false)
    isData(y) should be (true)
  }
}
