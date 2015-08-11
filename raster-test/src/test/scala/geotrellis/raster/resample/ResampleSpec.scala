package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class ResampleSpec extends FunSpec with Matchers {

  val B = 5 // value returned when resampling

  val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  val extent = Extent(1, 1, 2, 2)

  val resamp = new Resample(tile, extent) {

    protected def resampleValid(x: Double, y: Double): Int = B

    protected def resampleDoubleValid(x: Double, y: Double): Double = B

  }

  describe("when point outside extent it should return nodata") {

    it("should return NODATA when point is outside extent") {
      resamp.resample(0.99, 1.01) should be (NODATA)
      resamp.resample(1.01, 0.99) should be (NODATA)
      resamp.resample(2.01, 1.01) should be (NODATA)
      resamp.resample(1.01, 2.01) should be (NODATA)

      assert(resamp.resampleDouble(0.99, 1.01).isNaN)
      assert(resamp.resampleDouble(1.01, 0.99).isNaN)
      assert(resamp.resampleDouble(2.01, 1.01).isNaN)
      assert(resamp.resampleDouble(1.01, 2.01).isNaN)
    }

  }

  describe("when point inside extent it should return interpolation value") {

    it("should return interpolation value when point is on extent border") {
      resamp.resample(1, 1) should be (B)
      resamp.resample(1, 2) should be (B)
      resamp.resample(2, 1) should be (B)
      resamp.resample(2, 2) should be (B)
    }

    it("should return interpolation value when point is inside extent") {
      resamp.resample(1.01, 1.01) should be (B)
      resamp.resample(1.01, 1.99) should be (B)
      resamp.resample(1.99, 1.01) should be (B)
      resamp.resample(1.99, 1.99) should be (B)
    }

  }

}
