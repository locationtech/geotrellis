package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._
import org.scalatest._
import geotrellis.testkit._
import geotrellis.raster.IntArrayTile

class Atan2Spec extends FunSpec
  with Matchers
  with TestEngine
  with MultiBandTileBuilder {

  describe("ArcTan2 MultiBnadTile") {
    it("finds arctan2 of double multiband rasters") {

      val arr0 = Array(0.0, 1.0, 1.0, math.sqrt(3), Double.PositiveInfinity, Double.NaN)
      val arr1 = Array(-0.0, -1.0, -1.0, -math.sqrt(3), Double.NegativeInfinity, Double.NaN)
      val arr2 = Array(-0.0, -1.0, -1.0, -math.sqrt(3), Double.NegativeInfinity, Double.NaN)
      val arr3 = Array(0.0, 1.0, 1.0, math.sqrt(3), Double.PositiveInfinity, Double.NaN)

      val t0 = DoubleArrayTile(arr0, 3, 2)
      val t1 = DoubleArrayTile(arr1, 3, 2)
      val t2 = DoubleArrayTile(arr2, 3, 2)
      val t3 = DoubleArrayTile(arr3, 3, 2)
      val mb1 = MultiBandTile(Array(t0, t1, t2, t3))

      val arr4 = Array(0.0, math.sqrt(3), 1.0, 1.0, 1.0, 1.0)
      val arr5 = Array(1.0, math.sqrt(3), 1.0, 1.0, 1.0, 1.0)
      val arr6 = Array(-1.0, -math.sqrt(3), -1.0, -1.0, -1.0, -1.0)
      val arr7 = Array(-1.0, -math.sqrt(3), -1.0, -1.0, -1.0, -1.0)

      val t4 = DoubleArrayTile(arr4, 3, 2)
      val t5 = DoubleArrayTile(arr5, 3, 2)
      val t6 = DoubleArrayTile(arr6, 3, 2)
      val t7 = DoubleArrayTile(arr7, 3, 2)
      val mb2 = MultiBandTile(Array(t4, t5, t6, t7))

      val eArr0 = Array(0.0, 1.0 / 6, 1.0 / 4, 1.0 / 3, 0.5, Double.NaN)
      val eArr1 = Array(-0.0, -1.0 / 6, -1.0 / 4, -1.0 / 3, -0.5, -Double.NaN)
      val eArr2 = Array(-1.0, -5.0 / 6, -3.0 / 4, -2.0 / 3, -0.5, Double.NaN)
      val eArr3 = Array(1.0, 5.0 / 6, 3.0 / 4, 2.0 / 3, 0.5, -Double.NaN)

      val et0 = DoubleArrayTile(eArr0, 3, 2)
      val et1 = DoubleArrayTile(eArr1, 3, 2)
      val et2 = DoubleArrayTile(eArr2, 3, 2)
      val et3 = DoubleArrayTile(eArr3, 3, 2)

      val emb = MultiBandTile(Array(et0, et1, et2, et3))
      val expected = emb.mapDouble(x => math.Pi * x)
      val result = mb1.localAtan2(mb2)

      for (band <- 0 until result.bands) {
        for (col <- 0 until result.cols) {
          for (row <- 0 until result.rows) {
            val angle = result.getBand(band).getDouble(col, row)
            val epsilon = math.ulp(angle)
            if (mb1.getBand(band).getDouble(col, row).isNaN)
              angle.isNaN should be(true)
            else
              angle should be(expected.getBand(band).getDouble(col, row) +- epsilon)
          }
        }
      }
    }

    it("finds arctan2 of int multiband rasters") {
      val mb1 = arcIntMB
      val mb2 = intConstMB
      val result = mb1.localAtan2(mb2)
      val expectedAngles = Array(0.0, 0.25 * math.Pi, -0.25 * math.Pi, math.atan(2), math.atan(-2), Double.NaN)

      for (band <- 0 until result.bands) {
        for (col <- 0 until result.cols) {
          for (row <- 0 until result.rows) {
            val angle = result.getBand(band).getDouble(col, row)
            val epsilon = math.ulp(angle)
            if (mb1.getBand(band).getDouble(col, row).isNaN)
              angle.isNaN should be(true)
            else
              angle should be(expectedAngles(band) +- epsilon)
          }
        }
      }
    }

    it("finds arctan2 of a double multiband raster and an int multiband raster") {
      val m1 = arcDoubleMB
      val m2 = intConstMB

      val expectedAngles = Array(0.0, 0.25 * math.Pi, -0.25 * math.Pi, math.atan(2), math.atan(-2), Double.NaN)
      val result = m1.localAtan2(m2)

      for (band <- 0 until result.bands) {
        for (col <- 0 until result.cols) {
          for (row <- 0 until result.rows) {
            val angle = result.getBand(band).getDouble(col, row)
            val epsilon = math.ulp(angle)
            if (m1.getBand(band).getDouble(col, row).isNaN)
              angle.isNaN should be(true)
            else
              angle should be(expectedAngles(band) +- epsilon)
          }
        }
      }
    }

    it("finds arctan2 of a int multiband raster and a double multiband raster") {
      val m1 = arcIntMB
      val m2 = doubleConstMB

      val expectedAngles = Array(0.0, 0.25 * math.Pi, -0.25 * math.Pi, math.atan(2), math.atan(-2), Double.NaN)
      val result = m1.localAtan2(m2)

      for (band <- 0 until result.bands) {
        for (col <- 0 until result.cols) {
          for (row <- 0 until result.rows) {
            val angle = result.getBand(band).getDouble(col, row)
            val epsilon = math.ulp(angle)
            if (m1.getBand(band).getDouble(col, row).isNaN)
              angle.isNaN should be(true)
            else
              angle should be(expectedAngles(band) +- epsilon)
          }
        }
      }
    }

  }
}
