package geotrellis

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import geotrellis.testutil._
import geotrellis.raster.op._


@RunWith(classOf[JUnitRunner])
class AndThenTest extends FunSuite with ShouldMatchers {
  test("AndThen should forward operations forward") {
    case class TestOp1(x:Int) extends Op1(x)({
      (x) => {
        AndThen( local.Add(x + 2, 2) )
      }
    })

    val server = TestServer.server
    val r = server.run(TestOp1(3))
    assert(r == 7) 
  }
}
