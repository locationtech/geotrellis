/**************************************************************************
 * Copyright (c) 2014 Azavea.
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
 **************************************************************************/

package geotrellis.benchmark

import geotrellis._

import com.google.caliper.Param

object RasterForeach extends BenchmarkRunner(classOf[RasterForeach])
class RasterForeach extends OperationBenchmark {
  @Param(Array("64", "128", "256", "512", "1024"))
  var size:Int = 0
  
  var r:Raster = null

  override def setUp() {
    r = get(loadRaster("SBN_farm_mkt", size, size))
  }

  def timeRasterForeach(reps:Int) = run(reps)(rasterForeach)
  def rasterForeach = {
    var t = 0
    r.foreach(z => t += z)
    t
  }

  def timeRasterWhile(reps:Int) = run(reps)(rasterWhile)
  def rasterWhile = {
    var t = 0
    var i = 0
    val d = r.toArray
    val len = r.cols*r.rows
    while (i < len) {
      t += d(i)
      i += 1
    }
    t
  }
}
