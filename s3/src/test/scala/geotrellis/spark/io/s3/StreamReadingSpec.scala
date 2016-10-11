package geotrellis.spark.io.s3

import java.nio.file.{Paths, Files}
import com.amazonaws.services.s3.model._
import org.scalatest._


class StreamReadingSpec extends FunSpec with Matchers {
  val mockClient = new MockS3Client
  val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
  val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))

  mockClient.putObject(this.getClass.getSimpleName,
    "geotiff/all-ones.tif",
    geoTiffBytes)
  
  val request = new GetObjectRequest(this.getClass.getSimpleName, "geotiff/all-ones.tif")

  def testArrays[T](arr1: Array[T], arr2: Array[T]): Array[(T, T)] = {
    val zipped = arr1.zip(arr2)
    zipped.filter(x => x._1 != x._2)
  }

  def readRange(start: Long, end: Long): Array[Byte] = {
    val r = request.withRange(start, end)
    println(r.getRange.mkString(" "))
    val obj = mockClient.getObject(r)
    val stream = obj.getObjectContent
    val diff = (end - start).toInt
    val arr = Array.ofDim[Byte](diff)
    //stream.skip(start)
    stream.read(arr, 0, arr.length)
    stream.close()
    arr
  }

  it("should get the specified range") {
    val start: Long = 1
    val end: Long = 20
    val actual = readRange(start, end)
    val expected = geoTiffBytes.slice(start.toInt, end.toInt)

    println(actual.mkString(" "))
    println(expected.mkString(" "))

    val result = testArrays(actual, expected)

    result.length should be (0)
  }
}
