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
import geotrellis.raster.BitArrayRasterData
import geotrellis.TypeBit

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ArgWritableSpec extends FunSpec with MustMatchers with ShouldMatchers {
  describe("conversion from/to ArgWritable") {

    val cols = 2
    val rows = 2
    val size = cols * rows
    
    it("should convert from Array of ints to ArgWritable and back") {
      val expected = Array.fill(size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(IntArrayRasterData(expected, cols, rows)), TypeInt, cols, rows)
      expected should be(actual.asInstanceOf[IntArrayRasterData].array)
    }

    it("should convert from Array of shorts to ArgWritable and back") {
      val expected = Array.fill[Short](size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(ShortArrayRasterData(expected, cols, rows)), TypeShort, cols, rows)
      expected should be(actual.asInstanceOf[ShortArrayRasterData].array)
    }

    it("should convert from Array of doubles to ArgWritable and back") {
      val expected = Array.fill[Double](size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(DoubleArrayRasterData(expected, cols, rows)), TypeDouble, cols, rows)
      expected should be(actual.asInstanceOf[DoubleArrayRasterData].array)
    }

    it("should convert from Array of floats to ArgWritable and back") {
      val expected = Array.fill[Float](size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(FloatArrayRasterData(expected, cols, rows)), TypeFloat, cols, rows)
      expected should be(actual.asInstanceOf[FloatArrayRasterData].array)
    }

    it("should convert from Array of bytes to ArgWritable and back") {
      val expected = Array.fill[Byte](size)(1)
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(ByteArrayRasterData(expected, cols, rows)), TypeByte, cols, rows)
      expected should be(actual.asInstanceOf[ByteArrayRasterData].array)
    }

    it("should convert from Array of bytes (actually, bit masks) to ArgWritable and back") {
      val expected = Array.fill[Byte](size)(1)
      
      // bit mask length is 8x4 since there are 4 bytes of length 8 bits each
      val cols = 8
      val rows = 4
      
      val actual = ArgWritable.toRasterData(ArgWritable.toWritable(BitArrayRasterData(expected, cols, rows)), TypeBit, cols, rows)
      expected should be(actual.asInstanceOf[BitArrayRasterData].array)
    }
  }
}