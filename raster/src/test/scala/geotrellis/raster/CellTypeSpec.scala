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

import java.nio.file.Files
import java.nio.file.attribute.FileAttribute

import geotrellis.proj4.LatLng
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.writer.TiffTagFieldValue
import geotrellis.vector.Extent
import org.scalatest._

class CellTypeSpec extends FunSpec with Matchers with Inspectors {
  def roundTrip(ct: CellType) {
    withClue("fromName"){
      // Updated behavior.
      val str = ct.name
      val ctp = CellType.fromName(str)
      ctp should be (ct)
    }
  }

  def roundTripTiff(ct: CellType): Unit = {
    val tiffOut = GeoTiff(ArrayTile.alloc(ct, 1, 1), Extent(0, 0, 1, 1), LatLng)
    val path = Files.createTempFile("gt-", ".tiff")
    tiffOut.write(path.toString)

    val Left(tiffIn) = GeoTiff(path.toString)

    tiffIn.cellType should (be (tiffOut.cellType) or be (tiffOut.cellType.withDefaultNoData()))
    Files.delete(path)
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
        withClue(s"for cell type '$cellDef'") {
          forEvery(cellDef.range.testPoints) { nd ⇒
            withClue(s"for no data '$nd'") {
              val ct = cellDef(nd)
              roundTrip(ct)
              roundTripTiff(ct)
              assert(cellDef.toCode(nd) === ct.name)
              ct.widenedNoData(cellDef.alg) match {
                case WideIntNoData(wnd) ⇒ assert(wnd === nd)
                case WideDoubleNoData(wnd) ⇒ assert(wnd === nd)
              }
              assert(TiffTagFieldValue.createNoDataString(ct) === Some(nd.toString))
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
      def toCellEncoding(noData: NoDataEncoding): CellEncoding

      override def toString = baseCode
    }
    object CellDef {
      val all = Seq(UByteDef, ByteDef, UShortDef, ShortDef, IntDef, FloatDef, DoubleDef)
    }

    object UByteDef extends CellDef[Byte, Short] {
      val baseCode = "uint8"
      def apply(noData: Short) = UByteUserDefinedNoDataCellType(toCellEncoding(noData))
      val range = TestRange(0.toShort, (Byte.MaxValue * 2).toShort)
      def toCellEncoding(noData: Short) = noData.toByte
    }

    object ByteDef extends CellDef[Byte, Byte] {
      val baseCode = "int8"
      def apply(noData: Byte) = ByteUserDefinedNoDataCellType(toCellEncoding(noData))
      val range = TestRange(Byte.MinValue, Byte.MaxValue)
      def toCellEncoding(noData: Byte) = noData
    }

    object UShortDef extends CellDef[Short, Int] {
      val baseCode = "uint16"
      def apply(noData: Int) = UShortUserDefinedNoDataCellType(toCellEncoding(noData))
      val range = TestRange(0, Short.MaxValue * 2)
      def toCellEncoding(noData: Int) = noData.toShort
    }

    object ShortDef extends CellDef[Short, Short] {
      val baseCode = "int16"
      def apply(noData: Short) = ShortUserDefinedNoDataCellType(toCellEncoding(noData))
      val range = TestRange(Short.MinValue, Short.MaxValue)
      def toCellEncoding(noData: Short) = noData
    }

    object IntDef extends CellDef[Int, Int] {
      val baseCode = "int32"
      def apply(noData: Int) = IntUserDefinedNoDataCellType(toCellEncoding(noData))
      val range = TestRange(Int.MinValue, Int.MaxValue)
      def toCellEncoding(noData: Int) = noData
    }

    object FloatDef extends CellDef[Float, Double] {
      val baseCode = "float32"
      def apply(noData: Double) = FloatUserDefinedNoDataCellType(toCellEncoding(noData))
      val range = new TestRange(Float.MinValue.toDouble, Float.MaxValue.toDouble) {
        override def middle = 0.0f
      }
      def toCellEncoding(noData: Double) = noData.toFloat
    }

    object DoubleDef extends CellDef[Double, Double] {
      val baseCode = "float64"
      def apply(noData: Double) = DoubleUserDefinedNoDataCellType(toCellEncoding(noData))
      val range = new TestRange(Double.MinValue, Double.MaxValue) {
        override def middle = 0.0
      }
      def toCellEncoding(noData: Double) = noData
    }
  }

  describe("CellType NoData query") {
    it("should report nodata value for UserDefinedNoData") {

      val noData: Int = 86
      // Construct our own set of NoData cell types
      val userDefinedCelltypes = CellType.noNoDataCellTypes
        .filter(_ != BitCellType)
        .map(_.withNoData(Some(noData)))

      forEvery(userDefinedCelltypes) {
        case c: UserDefinedNoData[_] ⇒ c.noDataValue match {
          case n: Byte ⇒ assert(n === noData.toByte)
          case n: Short ⇒ assert(n === noData.toShort)
          case n: Int ⇒ assert(n === noData.toInt)
          case n: Float ⇒ assert(n === noData.toFloat)
          case n: Double ⇒ assert(n === noData.toDouble)
        }
      }
    }
    it("should report nodata value for ConstantNoData") {
      forEvery(CellType.constantNoDataCellTypes) { _.noDataValue match {
        case n: Byte ⇒ assert(n === byteNODATA || n === ubyteNODATA)
        case n: Short ⇒ assert(n === shortNODATA || n === ushortNODATA)
        case n: Int ⇒ assert(isNoData(n))
        case n: Float ⇒ assert(isNoData(n))
        case n: Double ⇒ assert(isNoData(n))
      }}
    }
  }
}
