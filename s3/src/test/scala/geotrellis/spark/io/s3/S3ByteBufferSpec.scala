package geotrellis.spark.io.s3.util

import geotrellis.util._
import geotrellis.spark.io.s3._

import java.nio.{ByteBuffer, ByteOrder}
import scala.collection.mutable._
import spire.syntax.cfor._

import org.scalatest._

class S3ByteReaderSpec extends FunSpec with Matchers with MockS3StreamBytes {
  val mock = new MockS3ByteReader()
  //println(mock.chunkBuffer.order)
  //val testBuffer = ByteBuffer.wrap(testArray)
  val testBuffer =
    Filesystem.toMappedByteBuffer("../../nlcd_2011_01_01.tif")
      .order(ByteOrder.LITTLE_ENDIAN)

  describe("Reading a file using S3ByteReader") {

    /*
    it("should be able to determine whether a point is within the buffer") {
      val actual = true
      val actual2 = false

      val expected = mock.isContained(4)
      val expected2 = mock.isContained(30)

      assert(actual == expected && actual2 == expected2)
    }

    it("should continue to read to the next MappedArray, Byte") {
      val expected = {
        mock.position(4)
        mock.get
      }

      val actual = {
        testBuffer.position(4)
        testBuffer.get
      }

      mock.position(0)
      testBuffer.position(0)

      assert(actual == expected)
    }

    it("should read through the whole array by, Byte") {
      val listBuffer = ListBuffer[Boolean]()

      cfor(0)(_ < objectLength.toInt, _ + 1){ i =>
        listBuffer += mock.get == testBuffer.get
      }
      
      cfor(0)(_ < mock.byteBuffer.capacity, _ + 1){ i =>
        listBuffer += mock.get == testBuffer.get
      }

      val result = listBuffer.filter(x => x == false)
      mock.position(0)
      testBuffer.position(0)

      assert(result.length == 0)
    }

    it("should continue to read to the next MappedArray, Char") {
      val expected = {
        mock.position(4)
        val first = mock.getChar
        
        mock.position(3)
        val second = mock.getChar

        (first, second)
      }

      val actual = {
        testBuffer.position(4)
        val first = testBuffer.getChar
        
        testBuffer.position(3)
        val second = testBuffer.getChar

        (first, second)
      }
      mock.position(0)
      testBuffer.position(0)

      assert(actual == expected)
    }
    */

    it("should read through the whole array by, Char") {
      val listBuffer = ListBuffer[Boolean]()
      //println(mock.chunkBuffer.order, testBuffer.order)

      /*
      cfor(0)(_ < objectLength.toInt - 1, _ + 2){ i =>
        listBuffer += mock.getChar == testBuffer.getChar
      }
      */
      
      cfor(0)(_ < objectLength.toInt, _ + 2){ i =>
        val result = mock.getChar == testBuffer.getChar
        if (!result)
          listBuffer += result
      }

      mock.position(0)
      testBuffer.position(0)

      assert(listBuffer.length == 0)
    }

    /*
    it("should continue to read to the next MappedArray, Short") {
      val expected = {
        mock.position(18)
        val first = mock.getShort
        
        mock.position(17)
        val second = mock.getShort

        (first, second)
      }

      val actual = {
        testBuffer.position(18)
        val first = testBuffer.getShort
        
        testBuffer.position(17)
        val second = testBuffer.getShort

        (first, second)
      }
      mock.position(0)
      testBuffer.position(0)

      assert(actual == expected)
    }
    */

    it("should read through the whole array by, Short") {
      val listBuffer = ListBuffer[Boolean]()

      /*
      cfor(0)(_ < objectLength.toInt - 1, _ + 2){ i =>
        listBuffer += mock.getShort == testBuffer.getShort
      }
      */
      
     cfor(0)(_ < objectLength.toInt, _ + 2){ i =>
        val result = mock.getShort == testBuffer.getShort
        if (!result)
          listBuffer += result
      }
      
      mock.position(0)
      testBuffer.position(0)

      assert(listBuffer.length == 0)
    }

    /*
    it("should continue to read to the next MappedArray, Int") {
      val expected = {

        mock.position(1)
        val first = mock.getInt
        mock.position(0)

        mock.position(2)
        val second = mock.getInt
        mock.position(0)

        mock.position(3)
        val third = mock.getInt
        mock.position(0)

        mock.position(4)
        val fourth = mock.getInt

        (first, second, third, fourth)
      }

      val actual = {

        testBuffer.position(1)
        val first = testBuffer.getInt

        testBuffer.position(2)
        val second = testBuffer.getInt

        testBuffer.position(3)
        val third = testBuffer.getInt

        testBuffer.position(4)
        val fourth = testBuffer.getInt

        (first, second, third, fourth)
      }

      mock.position(0)
      testBuffer.position(0)

      assert(actual == expected)
    }
    */

    it("should read through the whole array by, Int") {
      val listBuffer = ListBuffer[Boolean]()

      /*
      cfor(0)(_ < objectLength.toInt - 1, _ + 4){ i =>
        listBuffer += mock.getInt == testBuffer.getInt
      }
      */
     cfor(0)(_ < objectLength.toInt - 2, _ + 4){ i =>
        val result = mock.getInt == testBuffer.getInt
        if (!result)
          listBuffer += result
      }
      
      mock.position(0)
      testBuffer.position(0)

      assert(listBuffer.length == 0)
    }

    /*
    it("should continue to read to the next MappedArray, Float") {
      val expected = {

        mock.position(1)
        val first = mock.getFloat
        mock.position(0)

        mock.position(2)
        val second = mock.getFloat
        mock.position(0)

        mock.position(3)
        val third = mock.getFloat
        mock.position(0)

        mock.position(4)
        val fourth = mock.getFloat

        (first, second, third, fourth)
      }

      val actual = {

        testBuffer.position(1)
        val first = testBuffer.getFloat

        testBuffer.position(2)
        val second = testBuffer.getFloat

        testBuffer.position(3)
        val third = testBuffer.getFloat

        testBuffer.position(4)
        val fourth = testBuffer.getFloat

        (first, second, third, fourth)
      }

      mock.position(0)
      testBuffer.position(0)

      assert(actual == expected)
    }
    it("should read through the whole array by, Float") {
      //val listBuffer = ListBuffer[Boolean]()
      val listBuffer = ListBuffer[Float]()

      /*
      cfor(0)(_ < objectLength.toInt - 1, _ + 4){ i =>
        listBuffer += mock.getFloat == testBuffer.getFloat
      }
      */
      
      cfor(0)(_ < objectLength.toInt - 2, _ + 4){ i =>
        //val result = mock.getFloat == testBuffer.getFloat
      }

      println(mock.f)
      println(mock.position)
      
      mock.position(0)
      testBuffer.position(0)

      //assert(listBuffer.length == 0)
    }

    it("should continue to read to the next MappedArray, Double") {
      val expected = {

        mock.position(0)
        val first = mock.getDouble
        mock.position(0)

        mock.position(1)
        val second = mock.getDouble
        mock.position(0)

        mock.position(2)
        val third = mock.getDouble
        mock.position(0)

        mock.position(3)
        val fourth = mock.getDouble
        mock.position(0)
        
        mock.position(4)
        val fith = mock.getDouble

        (first, second, third, fourth, fith)
      }

      val actual = {

        testBuffer.position(0)
        val first = testBuffer.getDouble

        testBuffer.position(1)
        val second = testBuffer.getDouble

        testBuffer.position(2)
        val third = testBuffer.getDouble

        testBuffer.position(3)
        val fourth = testBuffer.getDouble

        testBuffer.position(4)
        val fith = testBuffer.getDouble

        (first, second, third, fourth, fith)
      }

      mock.position(0)
      testBuffer.position(0)

      assert(actual == expected)
    }
    */
    
    it("should read through the whole array by, Double") {
      val listBuffer = ListBuffer[Boolean]()

      /*
      cfor(0)(_ < objectLength.toInt - 6, _ + 8){ i =>
        listBuffer += mock.getDouble == testBuffer.getDouble
      }
      */
      
     cfor(0)(_ < objectLength.toInt - 2, _ + 8){ i =>
        val result = mock.getDouble == testBuffer.getDouble
        if (!result)
          listBuffer += result
      }
      
      mock.position(0)
      testBuffer.position(0)

      assert(listBuffer.length == 0)
    }

    /*
    it("should continue to read to the next MappedArray, Long") {
      val expected = {

        mock.position(0)
        val first = mock.getLong
        mock.position(0)

        mock.position(1)
        val second = mock.getLong
        mock.position(0)

        mock.position(2)
        val third = mock.getLong
        mock.position(0)

        mock.position(3)
        val fourth = mock.getLong
        mock.position(0)
        
        mock.position(4)
        val fith = mock.getLong

        (first, second, third, fourth, fith)
      }

      val actual = {

        testBuffer.position(0)
        val first = testBuffer.getLong

        testBuffer.position(1)
        val second = testBuffer.getLong

        testBuffer.position(2)
        val third = testBuffer.getLong

        testBuffer.position(3)
        val fourth = testBuffer.getLong

        testBuffer.position(4)
        val fith = testBuffer.getLong

        (first, second, third, fourth, fith)
      }

      mock.position(0)
      testBuffer.position(0)

      assert(actual == expected)
    }
    */
    
    it("should read through the whole array by, Long") {
      val listBuffer = ListBuffer[Boolean]()

      /*
      cfor(0)(_ < objectLength.toInt - 6, _ + 8){ i =>
        listBuffer += mock.getLong == testBuffer.getLong
      }
      */

      cfor(0)(_ < objectLength.toInt - 2, _ + 8){ i =>
        val result = mock.getLong == testBuffer.getLong
        if (!result)
          listBuffer += result
      }
      
      mock.position(0)
      testBuffer.position(0)

      assert(listBuffer.length == 0)
    }
  }
}
