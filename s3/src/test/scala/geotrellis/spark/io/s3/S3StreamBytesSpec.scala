package geotrellis.spark.io.s3.util

import scala.collection.mutable._
import spire.syntax.cfor._

import org.scalatest._

class StreamTester(chunkSize: Int, testArray: Array[Byte])
  extends MockS3StreamBytes(chunkSize, testArray)

class S3StreamBytesSpec extends FunSpec {
  val testArray = Array[Byte](
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
    10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
    20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
    30, 31, 32, 33, 34, 35, 36, 37, 38, 39)

  val chunkSize = 10

  val tester = new StreamTester(chunkSize, testArray)

  def arraysMatch[A <: Any](a1: Array[A], a2: Array[A]): Boolean = {
    val zipped = a1 zip a2
    val result = zipped.filter(x => x._1 != x._2)

    if (result.length == 0) true else false
  }

  describe("Readig the Stream from S3") {

    it("should be able to create a default chunk from the begininng") {
      val actual = tester.getArray
      val expected = Array[Byte](
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

      assert(arraysMatch(expected, actual))
    }

    it("should not read past the total file length") {
      val actual = tester.getArray(30, 50)
      val expected = Array[Byte](
        30, 31, 32, 33, 34, 35, 36, 37, 38, 39)

      assert(arraysMatch(expected, actual))
    }

    it("should have the correct offsets") {
      val listBuffer = ListBuffer[Long]()
      var counter = 0

      cfor(0)(_ < testArray.length, _ + chunkSize){i =>
        listBuffer += tester.getMappedArray(counter).head._1
        counter += chunkSize
      }

      val actual = listBuffer.toArray
      val expected = Array[Long](0, 10, 20, 30)

      assert(arraysMatch(expected, actual))
    }

    //it("should access the array the correct number of times")
  }
}
