package geotrellis.raster

import org.scalatest._

class CellTypeSpec extends FunSpec with Matchers {
  describe("CellType") {
    it("should union cells correctly under various circumstance") {
      ShortCellType.union(IntCellType) should be (IntCellType)
      DoubleCellType.union(FloatCellType) should be (DoubleCellType)
      ShortCellType.union(ShortCellType) should be (ShortCellType)
      IntCellType.union(FloatCellType) should be (FloatCellType)
      FloatCellType.union(IntCellType) should be (FloatCellType)
    }

    it("should intersect cells correctly under various circumstances") {
      ShortCellType.intersect(IntCellType) should be (ShortCellType)
      DoubleCellType.intersect(FloatCellType) should be (FloatCellType)
      ShortCellType.intersect(ShortCellType) should be (ShortCellType)
      IntCellType.intersect(FloatCellType) should be (IntCellType)
      FloatCellType.intersect(IntCellType) should be (IntCellType)
    }
    def roundTrip(ct: CellType) {
      val str = ct.toString
      val ctp = CellType.fromString(str)
      ctp should be (ct)
    }

    it("should serialize float64ud123") {
      roundTrip(DoubleUserDefinedNoDataCellType(123))
    }

    it("should serialize float64ud123.3") {
      roundTrip(DoubleUserDefinedNoDataCellType(123.3))
    }

    it("should serialize float64ud1e12") {
      roundTrip(DoubleUserDefinedNoDataCellType(1e12))
    }

    it("should serialize float64ud-1e12") {
      roundTrip(DoubleUserDefinedNoDataCellType(-1e12))
    }

    it("should serialize negative float64ud") {
      roundTrip(DoubleUserDefinedNoDataCellType(-1.7E308))
    }

    it("should serialize Float.MinValue value float64ud") {
      roundTrip(DoubleUserDefinedNoDataCellType(Double.MinValue))
    }

    it("should serialize Float.MaxValue value float64ud") {
      roundTrip(DoubleUserDefinedNoDataCellType(Double.MaxValue))
    }

    it("should serialize float64udInfinity") {
      roundTrip(DoubleUserDefinedNoDataCellType(Double.PositiveInfinity))
    }

    it("should serialize float64ud-Infinity") {
      roundTrip(DoubleUserDefinedNoDataCellType(Double.NegativeInfinity))
    }

    it("should read float64udNaN as float64") {
      CellType.fromString(DoubleUserDefinedNoDataCellType(Double.NaN).toString) should be (DoubleConstantNoDataCellType)
    }

    //----
    it("should serialize float32ud123") {
      roundTrip(FloatUserDefinedNoDataCellType(123f))
    }

    it("should serialize float32ud123.3") {
      roundTrip(FloatUserDefinedNoDataCellType(123.3f))
    }

    it("should serialize float32ud1e12") {
      roundTrip(FloatUserDefinedNoDataCellType(1e12f))
    }

    it("should serialize float32ud-1e12") {
      roundTrip(FloatUserDefinedNoDataCellType(-1e12f))
    }

    it("should serialize Float.MinValue value float32ud") {
      roundTrip(FloatUserDefinedNoDataCellType(Float.MinValue))
    }

    it("should serialize Float.MaxValue value float32ud") {
      roundTrip(FloatUserDefinedNoDataCellType(Float.MaxValue))
    }

    it("should serialize float32udInfinity") {
      roundTrip(FloatUserDefinedNoDataCellType(Float.PositiveInfinity))
    }

    it("should serialize float32ud-Infinity") {
      roundTrip(FloatUserDefinedNoDataCellType(Float.NegativeInfinity))
    }

    it("should read float32udNaN as float32") {
      CellType.fromString(FloatUserDefinedNoDataCellType(Float.NaN).toString) should be (FloatConstantNoDataCellType)
    }

  }
}
