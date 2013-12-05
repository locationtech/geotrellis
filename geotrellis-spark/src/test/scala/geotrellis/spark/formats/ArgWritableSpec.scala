package geotrellis.spark.formats

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

import geotrellis.TypeByte
import geotrellis.TypeDouble
import geotrellis.TypeFloat
import geotrellis.TypeInt
import geotrellis.TypeShort
import geotrellis.raster.ByteArrayRasterData
import geotrellis.raster.DoubleArrayRasterData
import geotrellis.raster.FloatArrayRasterData
import geotrellis.raster.IntArrayRasterData
import geotrellis.raster.ShortArrayRasterData

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ArgWritableSpec extends FunSpec with MustMatchers with ShouldMatchers {
  describe("conversion from/to ArgWritable") {
    
    val cols = 2
    val rows = 2
    val Size = cols * rows
    
    it("should convert from Array of ints to ArgWritable and back") {
      val expected = Array.fill(Size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(IntArrayRasterData(expected, 2, 2)), TypeInt, cols, rows)
      expected should be(actual.asInstanceOf[IntArrayRasterData].array)
    }

    it("should convert from Array of shorts to ArgWritable and back") {
      val expected = Array.fill[Short](Size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(ShortArrayRasterData(expected, 2, 2)), TypeShort, cols, rows)
      expected should be(actual.asInstanceOf[ShortArrayRasterData].array)
    }

    it("should convert from Array of doubles to ArgWritable and back") {
      val expected = Array.fill[Double](Size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(DoubleArrayRasterData(expected, 2, 2)), TypeDouble, cols, rows)
      expected should be(actual.asInstanceOf[DoubleArrayRasterData].array)
    }

    it("should convert from Array of floats to ArgWritable and back") {
      val expected = Array.fill[Float](Size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(FloatArrayRasterData(expected, 2, 2)), TypeFloat, cols, rows)
      expected should be(actual.asInstanceOf[FloatArrayRasterData].array)
    }

    it("should convert from Array of bytes to ArgWritable and back") {
      val expected = Array.fill[Byte](Size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(ByteArrayRasterData(expected, 2, 2)), TypeByte, cols, rows)
      expected should be(actual.asInstanceOf[ByteArrayRasterData].array)
    }
  }
}