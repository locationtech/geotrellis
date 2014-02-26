package geotrellis.spark

import geotrellis.spark.rdd.RasterRDD
import org.scalatest.matchers.ShouldMatchers

trait RasterRDDMatchers extends ShouldMatchers {
  
  /* 
   * Takes a 3-tuple, min, max, and count and checks
   * a. if every tile has a min/max value set to those passed in, 
   * b. if number of tiles == count
   */  
  def shouldBe(rdd: RasterRDD, minMaxCount: Tuple3[Int, Int, Long]): Unit = {
    val res = rdd.map { case (tileId, raster) => raster.findMinMax }.collect
    val (min, max, count) = minMaxCount
    //if(print) println("======" + res.foreach(println(_)) + "=========")
    res.count(_ == (min, max)) should be(count)
    res.length should be(count)
  }

  /* 
   * Takes a value and a count and checks
   * a. if every pixel == value, and
   * b. if number of tiles == count
   */   
  def shouldBe(rdd: RasterRDD, value: Int, count: Int): Unit = {
    val res = rdd.map { case (tileId, raster) => raster }.collect
    res.foreach(r => {
      for (col <- 0 until r.cols) {
        for (row <- 0 until r.rows) {
          r.get(col, row) should be(value)
        }
      }
    })

    res.length should be(count)
  }
}