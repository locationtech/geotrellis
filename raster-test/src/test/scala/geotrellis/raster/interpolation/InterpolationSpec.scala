package geotrellis.raster.interpolation

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class InterpolationSpec extends FunSpec with Matchers {

  val B = 5 // value returned when interpolating

  val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  val extent = Extent(1, 1, 2, 2)

  val interp = new Interpolation(tile, extent) {

    protected def interpolateValid(x: Double, y: Double): Int = B

    protected def interpolateDoubleValid(x: Double, y: Double): Double = B

  }

  describe("when point outside extent it should return nodata") {

    it("should return NODATA when point is outside extent") {
      interp.interpolate(0.99, 1.01) should be (NODATA)
      interp.interpolate(1.01, 0.99) should be (NODATA)
      interp.interpolate(2.01, 1.01) should be (NODATA)
      interp.interpolate(1.01, 2.01) should be (NODATA)

      assert(interp.interpolateDouble(0.99, 1.01).isNaN)
      assert(interp.interpolateDouble(1.01, 0.99).isNaN)
      assert(interp.interpolateDouble(2.01, 1.01).isNaN)
      assert(interp.interpolateDouble(1.01, 2.01).isNaN)
    }

  }

  describe("when point inside extent it should return interpolation value") {

    it("should return interpolation value when point is on extent border") {
      interp.interpolate(1, 1) should be (B)
      interp.interpolate(1, 2) should be (B)
      interp.interpolate(2, 1) should be (B)
      interp.interpolate(2, 2) should be (B)
    }

    it("should return interpolation value when point is inside extent") {
      interp.interpolate(1.01, 1.01) should be (B)
      interp.interpolate(1.01, 1.99) should be (B)
      interp.interpolate(1.99, 1.01) should be (B)
      interp.interpolate(1.99, 1.99) should be (B)
    }

  }

}
