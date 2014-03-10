package geotrellis

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import geotrellis.testkit._
import geotrellis.raster.op._


class AndThenTest extends FunSuite 
                     with ShouldMatchers 
                     with TestServer {
  test("AndThen should forward operations forward") {
    case class TestOp1(x:Int) extends Op1(x)({x:Int => AndThen(x + 4)})

    val r = get(TestOp1(3))
    assert(r == 7) 
  }
}
