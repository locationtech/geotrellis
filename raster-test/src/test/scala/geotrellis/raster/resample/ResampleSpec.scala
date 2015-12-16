package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class ResampleSpec extends FunSpec with Matchers {

  val B = 5 // value returned when resampling

  val tile = ArrayTile(Array[Int](100, 100, 100, 100), 2, 2)
  val extent = Extent(1, 1, 2, 2)
  val cellSize = CellSize(extent, 1, 2)

  val resamp = new AggregateResample(tile, extent, cellSize) {

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

  describe("aggregate resampling requires assembling a list of contributing cells") {

    it("should assemble contributing cell indexes correctly for a given cellsize") {
      /* Given an initial tile of 2columns/2rows and resizing it to 1column/2rows
       * we should expect that the contributing cells at (0, 0) include only (0, 0)
       * we should expect that the contributing cells at (1.5, 1.25) include 0,0 and 1,0
       * we should expect that the contributing cells at (1.5, 1.75) include 0,1 and 1,1
       */
      resamp.contributions(0, 0) should be (Vector.empty)
      resamp.contributions(1.5, 1.25) should be (Vector((0, 0), (1, 0)))
      resamp.contributions(1.5, 1.75) should be (Vector((0, 1), (1, 1)))
    }
  }


}
