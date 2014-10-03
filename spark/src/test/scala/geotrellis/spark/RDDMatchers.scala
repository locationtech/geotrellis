/*
 * Copyright (c) 2014 DigitalGlobe.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark

import org.scalatest._

trait RasterRDDMatchers extends Matchers {
  
  /* 
   * Takes a 3-tuple, min, max, and count and checks
   * a. if every tile has a min/max value set to those passed in, 
   * b. if number of tiles == count
   */  
  def shouldBe[K](rdd: RasterRDD[K], minMaxCount: (Int, Int, Long)): Unit = {
    val res = rdd.map(_.tile.findMinMax).collect
    val (min, max, count) = minMaxCount
    res.count(_ == (min, max)) should be(count)
    res.length should be(count)
  }

  /* 
   * Takes a value and a count and checks
   * a. if every pixel == value, and
   * b. if number of tiles == count
   */   
  def shouldBe[K](rdd: RasterRDD[K], value: Int, count: Int): Unit = {
    val res = rdd.map(_.tile).collect

    res.foreach { r =>
      for (col <- 0 until r.cols) {
        for (row <- 0 until r.rows) {
          r.get(col, row) should be(value)
        }
      }
    }

    res.length should be(count)
  }
}
