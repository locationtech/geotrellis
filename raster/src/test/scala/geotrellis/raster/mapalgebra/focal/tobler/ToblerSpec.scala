package geotrellis.raster.mapalgebra.focal.tobler

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._
import geotrellis.raster.mapalgebra.focal.tobler._
import geotrellis.raster.testkit._
import geotrellis.util.Constants.{FLOAT_EPSILON => EPSILON}

import scala.math.{abs, exp}

import org.scalatest._

class ToblerSpec extends FunSpec with Matchers with RasterMatchers with FocalOpSpec {

  describe("Tobler") {
    it ("should produce expected results") {
      val elev = DoubleArrayTile.ofDim(5, 5).mapDouble{ (x, _, _) => x }
      assert(elev.get(3, 3) == 3)

      val slopeAngle = elev.slope(CellSize(1, 1))
      assert(abs(slopeAngle.get(3,3) - 45) < EPSILON)
      val slope = (slopeAngle * math.Pi / 180.0).localTan
      assert(abs(slope.getDouble(3, 3) - 1.0) < EPSILON)

      val tobler = elev.tobler(CellSize(1, 1))

      (abs(tobler.getDouble(3,3) - 6 * exp(-3.5 * abs(1 + 0.05))) < EPSILON) should be (true)
    }
  }

}
