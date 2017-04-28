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

    implicit val doubleAsIntegral = scala.math.Numeric.DoubleAsIfIntegral
    implicit val floatAsIntegral = scala.math.Numeric.FloatAsIfIntegral

    it("should handle encoding no data types across valid bounds") {
      type PhantomCell = AnyVal
      type PhantomNoData = AnyVal

      forEvery(CellDef.all) { cd ⇒
        val cellDef = cd.asInstanceOf[CellDef[PhantomCell, PhantomNoData]]
        withClue("Cell type " + cellDef) {
          val range = cellDef.range
          forEvery(cellDef.range.testPoints) { nd ⇒
            withClue("No data " + nd) {
              val ct = cellDef(nd)
              roundTrip(ct)
//          assert(cellDef.toCode(nd) === ct.name)
//          println(ct.widenedNoData(cellDef.alg))
            }
          }
        }
      }
    }
    abstract class RangeAlgebra[T: Integral] {
      val alg = implicitly[Integral[T]]
      import alg._
      val one = alg.one
      val twice =  one + one
    }

    case class TestRange[Encoding: Integral](min: Encoding, max: Encoding) extends RangeAlgebra[Encoding]{
      import alg._
      def width = max - min
      def middle = width / twice
      def testPoints = Seq(
        min, min + one, middle - one, middle, middle + one, max - one, max
      )
    }

    abstract class CellDef[CellEncoding: Integral, NoDataEncoding: Integral] extends RangeAlgebra[CellEncoding] {
      val range: TestRange[NoDataEncoding]
      val baseCode: String
      def apply(noData: NoDataEncoding): CellType with UserDefinedNoData[CellEncoding]
      def toCode(noData: NoDataEncoding): String = {
        s"${baseCode}ud${noData}"
      }

      override def toString = baseCode
    }
    object CellDef {
      val all = Seq(UByteDef, ByteDef, UShortDef, ShortDef, IntDef, FloatDef, DoubleDef)
    }

    object UByteDef extends CellDef[Byte, Short] {
      val baseCode = "uint8"
      def apply(noData: Short) = UByteUserDefinedNoDataCellType(noData.toByte)
      val range = TestRange(0.toShort, (Byte.MaxValue * 2).toShort)
    }

    object ByteDef extends CellDef[Byte, Byte] {
      val baseCode = "int8"
      def apply(noData: Byte) = ByteUserDefinedNoDataCellType(noData.toByte)
      val range = TestRange(Byte.MinValue, Byte.MaxValue)
    }

    object UShortDef extends CellDef[Short, Int] {
      val baseCode = "uint16"
      def apply(noData: Int) = UShortUserDefinedNoDataCellType(noData.toShort)
      val range = TestRange(0, Short.MaxValue * 2)
    }

    object ShortDef extends CellDef[Short, Short] {
      val baseCode = "int16"
      def apply(noData: Short) = ShortUserDefinedNoDataCellType(noData)
      val range = TestRange(Short.MinValue, Short.MaxValue)
    }

    object IntDef extends CellDef[Int, Int] {
      val baseCode = "int32"
      def apply(noData: Int) = IntUserDefinedNoDataCellType(noData)
      val range = TestRange(Int.MinValue, Int.MaxValue)
    }

    object FloatDef extends CellDef[Float, Float] {
      val baseCode = "float32"
      def apply(noData: Float) = FloatUserDefinedNoDataCellType(noData)
      val range = new TestRange(Float.MinValue, Float.MaxValue) {
        override def middle = 0.0f
      }
    }

    object DoubleDef extends CellDef[Double, Double] {
      val baseCode = "float64"
      def apply(noData: Double) = DoubleUserDefinedNoDataCellType(noData)
      val range = new TestRange(Double.MinValue, Double.MaxValue) {
        override def middle = 0.0
      }
    }
  }

}
