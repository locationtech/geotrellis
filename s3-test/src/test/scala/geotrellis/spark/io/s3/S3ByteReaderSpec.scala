package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.testkit._

import java.nio.{ByteBuffer, ByteOrder}
import scala.collection.mutable._
import spire.syntax.cfor._

import org.scalatest._

class Tester(mock: MockS3ByteReader, testBuffer: ByteBuffer) {

  def readNext[T](startPoint: Int, t: T): Int =
    readNext(startPoint, mock.length, t)

  def readNext[T](startPoint: Int, endPoint: Int, t: T): Int = {
    def matcher: Boolean =
      t match {
        case Byte => mock.get == testBuffer.get
        case Char => mock.getChar == testBuffer.getChar
        case Short => mock.getShort == testBuffer.getShort
        case Int => mock.getInt == testBuffer.getInt
        case Float => {
          val m = mock.getFloat
          val t = testBuffer.getFloat
          (m == t || (m.isNaN && t.isNaN))
        }
        case Double => mock.getDouble == testBuffer.getDouble
        case Long => mock.getLong == testBuffer.getLong
      }
    
    val iterator: Int =
      t match {
        case Byte => 1
        case Char => 2
        case Short => 2
        case Int => 4
        case Float => 4
        case Double => 8
        case Long => 8
      }

    val listBuffer = ListBuffer[Boolean]()
    mock.position(startPoint)
    testBuffer.position(startPoint)

    cfor(startPoint)(_ < endPoint, _ + iterator) { i =>
      val result = matcher

      if(!result)
        listBuffer += result
    }

    mock.position(0)
    testBuffer.position(0)
    
    listBuffer.filter(x => !x)
    listBuffer.length
  }
}

class S3ByteReaderSpec extends FunSpec with Matchers {

  describe("general behavior") {
    val chunkSize = 9
    val testArray: Array[Byte] =
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31)

    val s3Stream = new MockS3ArrayBytes(chunkSize, testArray)
    val mock = new MockS3ByteReader(s3Stream, None)

    def equalArrays(arr1: Array[Byte], arr2: Array[Byte]): Array[(Byte, Byte)] = {
      val zipped = arr1.zip(arr2)
      val result = zipped.filter(x => x._1 != x._2)
      result
    }

    it("should return the correct position") {
      mock.position(15)
      mock.position(22)

      mock.position should be (22)
    }
    
    it("should be able to determine whether a point is within the buffer") {
      mock.position(0)
      val positions = Array(0, 1, 3, 5, 15, 27, 32, 52, 78)
      val result = positions.filter(x => mock.isContained(x))

      result.length should be (4)
    }

    it("should access the array a certain number of times") {
      mock.position(11)
      mock.accessCount = 0

      cfor(11)(_ < testArray.length - 1, _ + 4) {i =>
        mock.getInt
      }

      mock.accessCount should be (2)
    }

    it("should apped the correct bytes to the next chunk, byte") {
      mock.position(0)
      mock.position(8)
      val actual = Array.ofDim[Array[Byte]](2)

      cfor(0)(_ < 2, _ + 1) { i =>
        mock.get
        actual(i) = mock.chunkBuffer.array
      }

      val expected: Array[Array[Byte]] = Array(Array(0, 1, 2, 3, 4, 5, 6, 7, 8),
        Array(9, 10, 11, 12, 13, 14, 15, 16, 17))

      val result = equalArrays(expected.flatten, actual.flatten)

      result.length should be (0)
    }
    
    it("should apped the correct bytes to the next chunk, char") {
      mock.position(0)
      mock.position(8)
      val actual = Array.ofDim[Array[Byte]](2)

      cfor(0)(_ < 2, _ + 1) { i =>
        mock.getChar
        actual(i) = mock.chunkBuffer.array
      }

      val expected: Array[Array[Byte]] = Array(
        Array(8, 9, 10, 11, 12, 13, 14, 15, 16),
        Array(8, 9, 10, 11, 12, 13, 14, 15, 16))

      val result = equalArrays(expected.flatten, actual.flatten)

      result.length should be (0)
    }
  }

  describe("Reading a byte array") {
    val chunkSize = 9
    val testArray: Array[Byte] =
      Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31)

    val s3Stream = new MockS3ArrayBytes(chunkSize, testArray)
    val mock = new MockS3ByteReader(s3Stream, None)
    val testBuffer = ByteBuffer.wrap(testArray)

    val tester = new Tester(mock, testBuffer)

    it("should continue to read to the next MappedArray, Byte") {
      val result = tester.readNext(0, Byte)

      result should be (0)
    }

    it("should continue to read to the next MappedArray, Char") {
      val result = tester.readNext(0, testArray.length, Char)

      result should be (0)
    }

    it("should continue to read to the next MappedArray, Short") {
      val result = tester.readNext(0, testArray.length, Short)

      result should be (0)
    }

    it("should continue to read to the next MappedArray, Int") {
      val result = tester.readNext(0, testArray.length, Int)

      result should be (0)
    }
    
    it("should continue to read to the next MappedArray, Float") {
      val result = tester.readNext(0, testArray.length, Float)

      result should be (0)
    }
    
    it("should continue to read to the next MappedArray, Double") {
      val result = tester.readNext(0, testArray.length, Double)

      result should be (0)
    }
    
    it("should continue to read to the next MappedArray, Long") {
      val result = tester.readNext(0, testArray.length, Long)

      result should be (0)
    }
  }

  describe("Reading from a local geotiff") {
    val testBuffer =
      Filesystem
        .toMappedByteBuffer("raster-test/data/geotiff-test-files/multi-tag.tif")
    
    val testArray =
      Filesystem
        .slurp("raster-test/data/geotiff-test-files/multi-tag.tif")
    
    val chunkSize = 11
  
    val byteOrder: ByteOrder =
      (testArray(0).toChar, testArray(1).toChar) match {
        case ('I', 'I') =>  ByteOrder.LITTLE_ENDIAN
        case ('M', 'M') => ByteOrder.BIG_ENDIAN
        case _ => throw new Exception("incorrect byte order")
      }

    val s3Stream = new MockS3ArrayBytes(chunkSize, testArray)
    val mock = new MockS3ByteReader(s3Stream, Some(byteOrder))
    val tester = new Tester(mock, testBuffer.order(byteOrder))

    it("should continue to read to the next MappedArray, Byte") {
      val result = tester.readNext(0, Byte)

      result should be (0)
    }

    it("should continue to read to the next MappedArray, Char") {
      val result = tester.readNext(0, Char)
      
      result should be (0)
    }
    
    it("should continue to read to the next MappedArray, Short") {
      val result = tester.readNext(0, Short)

      result should be (0)
    }
    
    it("should continue to read to the next MappedArray, Int") {
      val result = tester.readNext(0, Int)

      result should be (0)
    }

    it("should continue to read to the next MappedArray, Float") {
      val result = tester.readNext(0, Float)

      result should be (0)
    }

    it("should continue to read to the next MappedArray, Double") {
      val result = tester.readNext(0, Double)

      result should be (0)
    }
    
    it("should continue to read to the next MappedArray, Long") {
      val result = tester.readNext(0, Long)

      result should be (0)
    }
  }
}
