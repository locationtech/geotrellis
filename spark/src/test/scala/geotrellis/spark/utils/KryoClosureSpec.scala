package geotrellis.spark.utils

import geotrellis.spark._
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.RasterRDD
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class OptimusPrime(val prime: Int) extends Function1[Int, Int] {
  def apply(x: Int): Int = prime + x
}
class KryoClosureSpec extends FunSpec
  with TestEnvironment
  with TestFiles
  with RasterRDDMatchers {
  val transformer = new OptimusPrime(7)
  val numbers = Array.fill(10)(10)

  describe("KryoClosure") {
    val rdd = sc.parallelize(numbers, 1)

    it("should be better then Java serialization") {
      intercept[org.apache.spark.SparkException] {
        rdd.map(transformer).collect
      }
    }

    it("should be totally awesome at serialization"){
      val out = rdd.map(KryoClosure(transformer))
      out.collect should be (Array.fill(10)(17))
    }
  }
}
