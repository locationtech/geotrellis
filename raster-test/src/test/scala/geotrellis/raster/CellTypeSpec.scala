/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import org.scalatest._

class CellTypeSpec extends FunSpec with Matchers with Inspectors {
  def roundTrip(ct: CellType) {
    withClue("fromName"){
      // Updated behavior.
      val str = ct.name
      val ctp = CellType.fromName(str)
      ctp should be (ct)
    }
    withClue("fromString"){
      // Tests backward compatibility.
      val str = ct.toString
      //noinspection ScalaDeprecation
      val ctp = CellType.fromString(str)
      ctp should be (ct)
    }
  }

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
      CellType.fromName(DoubleUserDefinedNoDataCellType(Double.NaN).toString) should be (DoubleConstantNoDataCellType)
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
      CellType.fromName(FloatUserDefinedNoDataCellType(Float.NaN).toString) should be (FloatConstantNoDataCellType)
    }

  }

  describe("CellType Bounds checking") {

    //    it("should serialize uint8ud with various ND values") {
    //
    //      val ndVals = Seq(0, 1, Byte.MaxValue/2, Byte.MaxValue - 1, Byte.MaxValue, Byte.MaxValue * 2)
    //      forEvery(ndVals) { nd ⇒
    //        roundTrip(UByteUserDefinedNoDataCellType(nd.toByte))
    //      }
    //    }
    //
    //    it("should serialize int8ud with various ND values") {
    //
    //      val ndVals = Seq(0, 1, 127, 128, 255)
    //      forEvery(ndVals) { nd ⇒
    //        roundTrip(ByteUserDefinedNoDataCellType(nd.toByte))
    //      }
    //    }



    implicit val doubleAsIntegral = scala.math.Numeric.DoubleAsIfIntegral
    implicit val floatAsIntegral = scala.math.Numeric.FloatAsIfIntegral


    it("should handle encoding no data types across valid bounds") {
      forEvery(CellDef.all) { cellDef ⇒
        val cd = cellDef.asInstanceOf[CellDef[AnyVal]]
        forEvery(cd.testPoints) { nd ⇒
          val ct = cd(nd)
          assert(cd.toCode(nd) === ct.name)
          roundTrip(ct)
        }
      }
    }

    abstract class RangeAlgebra[T: Integral] {
      protected val alg = implicitly[Integral[T]]
      import alg._
      val one = alg.one
      val twice =  one + one
      val half = one / twice
    }

    case class TestRange[Encoding: Integral](min: Encoding, max: Encoding) extends RangeAlgebra[Encoding]{
      import alg._
      def width = max - min
      def middle = width * half
    }

    abstract class CellDef[T: Integral] extends RangeAlgebra[T] {
      import alg._
      type Encoding = T
      val rng: TestRange[Encoding]
      val baseCode: String
      def apply(noData: Encoding): CellType with UserDefinedNoData[_]
      def toCode(noData: Encoding): String = {
        s"${baseCode}ud${noData}"
      }
      def testPoints = Seq(
        rng.min, rng.min + one, rng.middle - one, rng.middle, rng.middle + one, rng.max - one, rng.max
      )
      override def toString = getClass.getSimpleName
    }
    object CellDef {
      val all = Seq(UByteDef, ByteDef, UShortDef, ShortDef, IntDef, FloatDef, DoubleDef)
    }

    object UByteDef extends CellDef[Short] {
      val baseCode = "uint8"
      def apply(noData: Short) = UByteUserDefinedNoDataCellType(noData.toByte)
      val rng = TestRange(0.toShort, (Byte.MaxValue * 2).toShort)
    }

    object ByteDef extends CellDef[Byte] {
      val baseCode = "int8"
      def apply(noData: Byte) = ByteUserDefinedNoDataCellType(noData.toByte)
      val rng = TestRange(Byte.MinValue, Byte.MaxValue)
    }

    object UShortDef extends CellDef[Int] {
      val baseCode = "uint16"
      def apply(noData: Int) = UShortUserDefinedNoDataCellType(noData.toShort)
      val rng = TestRange(0, Short.MaxValue * 2)
    }

    object ShortDef extends CellDef[Short] {
      val baseCode = "int16"
      def apply(noData: Short) = ShortUserDefinedNoDataCellType(noData)
      val rng = TestRange(Short.MinValue, Short.MaxValue)
    }

    object IntDef extends CellDef[Int] {
      val baseCode = "int32"
      def apply(noData: Int) = IntUserDefinedNoDataCellType(noData)
      val rng = TestRange(Int.MinValue, Int.MaxValue)
    }

    object FloatDef extends CellDef[Float] {
      val baseCode = "float32"
      def apply(noData: Float) = FloatUserDefinedNoDataCellType(noData)
      val rng = TestRange(Float.MinValue, Float.MaxValue)
    }

    object DoubleDef extends CellDef[Double] {
      val baseCode = "float64"
      def apply(noData: Double) = DoubleUserDefinedNoDataCellType(noData)
      val rng = TestRange(Double.MinValue, Double.MaxValue)
    }
  }

}
