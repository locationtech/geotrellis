/*
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
 */

package geotrellis.benchmark

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.engine.op.local._
import geotrellis.raster.stats._
import geotrellis.raster.render._

import com.google.caliper.Param

object WeightedAdd extends BenchmarkRunner(classOf[WeightedAdd])
class WeightedAdd extends OperationBenchmark {
  // val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  // val weights = Array(2, 1, 5, 2)

  // val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  // val weights = Array(2, 1, 5, 2)

  val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k",
                    "SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k",
                    "SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k",
                    "SBN_farm_mkt", "SBN_RR_stops_walk", "SBN_inc_percap", "SBN_street_den_1k")
  val weights = Array(2, 1, 5, 2,
                      2, 1, 5, 2,
                      2, 1, 5, 2,
                      2, 1, 5, 2)

  // val names = Array("SBN_farm_mkt", "SBN_RR_stops_walk")
  // val weights = Array(2, 3)

  @Param(Array("256", "512", "1024", "2048", "4096"))
  var size: Int = 0

  var source: RasterSource = null
  var sourceSeq: RasterSource = null

  override def setUp() {
    val n = names.length
    val re = getRasterExtent(names(0), size, size)

    source = 
      (0 until n).map(i => RasterSource(names(i), re) * weights(i))
                 .reduce(_ + _)

    sourceSeq = 
      (0 until n).map(i => RasterSource(names(i), re) * weights(i))
                 .localAdd

  }

  def timeWeightedAddSource(reps: Int) = run(reps)(weightedAddSource)
  def weightedAddSource = get(source)

  def timeWeightedAddSourceSeq(reps: Int) = run(reps)(weightedAddSourceSeq)
  def weightedAddSourceSeq = get(sourceSeq)
}
