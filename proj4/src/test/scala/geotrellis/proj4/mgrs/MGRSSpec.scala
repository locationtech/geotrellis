package geotrellis.proj4.mgrs

import org.scalatest.{FunSpec, Matchers}

class MGRSSpec extends FunSpec with Matchers {

  describe("MGRS") {
    it("should produce bounding boxes containing the original point") {
      val long = 360.0 * scala.util.Random.nextDouble - 180.0
      val lat = 164.0 * scala.util.Random.nextDouble - 80.0
      val results = for (accuracy <- 1 to 5) yield {
        val mgrsString = MGRS.longLatToMGRS(long, lat, 3)
        val bbox = MGRS.mgrsToBBox(mgrsString)
        bbox._1 <= long && long <= bbox._3 && bbox._2 <= lat && lat <= bbox._4
      }

      println(s"MGRS: Tested against long/lat ($long, $lat)")

      val testStat = results.reduce(_ && _)
      testStat should be (true)
    }
  }

}
