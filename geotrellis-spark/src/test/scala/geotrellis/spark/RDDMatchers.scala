package geotrellis.spark

import geotrellis.spark.rdd.RasterRDD
import org.scalatest.matchers.ShouldMatchers

trait RasterRDDMatchers extends ShouldMatchers {
  def shouldBe(rdd: RasterRDD, minMaxCount: Tuple3[Int, Int, Long]): Unit = {
    val res = rdd.map { case (tileId, raster) => raster.findMinMax }.collect
    val (min, max, count) = minMaxCount

    res.count(_ == (min, max)) should be(count)
    res.length should be(count)
  }
}