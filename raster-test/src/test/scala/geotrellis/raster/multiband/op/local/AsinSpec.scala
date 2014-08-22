package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

import org.scalatest._

import geotrellis.testkit._

class AsinSpec extends FunSpec
  with Matchers
  with TestEngine
  with MultiBandTileBuilder {

  describe("ArcSin MultiBandTile") {
    it("finds arcsin of a double multiband raster") {
      val arr0 = Array(0.0, 0.5, 1 / math.sqrt(2), math.sqrt(3) / 2, 1.0, Double.NaN, 0, 0, 0)
      val arr1 = Array(0.0, -0.5, -1 / math.sqrt(2), -math.sqrt(3) / 2, -1.0, Double.NaN, 0, 0, 0)
      val arr2 = Array(0.0, 0.5, 1 / math.sqrt(2), math.sqrt(3) / 2, 1.0, Double.NaN, 0, 0, 0)
      val arr3 = Array(0.0, -0.5, -1 / math.sqrt(2), -math.sqrt(3) / 2, -1.0, Double.NaN, 0, 0, 0)

      val t0 = DoubleArrayTile(arr0, 3, 3)
      val t1 = DoubleArrayTile(arr1, 3, 3)
      val t2 = DoubleArrayTile(arr2, 3, 3)
      val t3 = DoubleArrayTile(arr3, 3, 3)

      val mb = MultiBandTile(Array(t0, t1, t2, t3))

      val eArr0 = Array(0.0, 1.0 / 6, 1.0 / 4, 1.0 / 3, 0.5, Double.NaN, 0.0, 0.0, 0.0)
      val eArr1 = Array(-0.0, -1.0 / 6, -1.0 / 4, -1.0 / 3, -0.5, -Double.NaN, -0.0, -0.0, -0.0)
      val eArr2 = Array(0.0, 1.0 / 6, 1.0 / 4, 1.0 / 3, 0.5, Double.NaN, 0.0, 0.0, 0.0)
      val eArr3 = Array(-0.0, -1.0 / 6, -1.0 / 4, -1.0 / 3, -0.5, -Double.NaN, -0.0, -0.0, -0.0)

      val et0 = DoubleArrayTile(eArr0, 3, 3)
      val et1 = DoubleArrayTile(eArr1, 3, 3)
      val et2 = DoubleArrayTile(eArr2, 3, 3)
      val et3 = DoubleArrayTile(eArr3, 3, 3)

      val expected = MultiBandTile(Array(et0, et1, et2, et3)).mapDouble(x => math.Pi * x)
      val result = mb.localAsin

      for (band <- 0 until result.bands) {
        for (col <- 0 until result.cols) {
          for (row <- 0 until result.rows) {
            val angle = result.getBand(band).getDouble(col, row)
            val epsilon = math.ulp(angle)
            if (mb.getBand(band).getDouble(col, row).isNaN())
              angle.isNaN() should be(true)
            else
              angle should be(expected.getBand(band).getDouble(col, row) +- epsilon)
          }
        }
      }
    }

    it("is NaN when the absolute value of the cell of a double raster > 1") {
      val mb = absDubmb
      val result = mb.localAsin

      for (band <- 0 until result.bands) {
        for (col <- 0 until result.cols) {
          for (row <- 0 until result.rows) {
            if (mb.getBand(band).getDouble(col, row).isNaN() || mb.getBand(band).getDouble(col, row).abs > 1.0)
              result.getBand(band).getDouble(col, row).isNaN() should be(true)
          }
        }
      }
    }

    it("finds arccos of an int multiband raster") {
      val mb = arcMB
      val expectedAngles = Array(0.0, 0.5, -0.5, Double.NaN, Double.NaN, Double.NaN).map(x => x * math.Pi)
      val result = mb.localAsin

      for (band <- 0 until result.bands) {
        for (col <- 0 until result.cols) {
          for (row <- 0 until result.rows) {
            val angle = result.getBand(band).getDouble(col, row)
            val epsilon = math.ulp(angle)
            if (isNoData(mb.getBand(band).get(col, row)) || mb.getBand(band).getDouble(col, row).abs > 1)
              angle.isNaN() should be(true)
            else
              angle should be(expectedAngles(band) +- epsilon)
          }
        }
      }
    }

  }
}