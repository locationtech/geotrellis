/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.benchmark

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op.local
import geotrellis.io.LoadRaster

import com.google.caliper.Param

object DivideBenchmark extends BenchmarkRunner(classOf[DivideBenchmark])
class DivideBenchmark extends OperationBenchmark {
  val n = 10
  val name = "SBN_farm_mkt"

  @Param(Array("256","512", "1024", "2048", "4096", "8192"))
  var size:Int = 0

  var op:Op[Raster] = null
  var source:RasterSource = null

  override def setUp() {
    val re = getRasterExtent(name, size, size)
    val raster = get(LoadRaster(name,re))
    op = local.Divide(raster, n)

    source = 
      RasterSource(name,re)
        .cached
        .localDivide(n)
  }

  def timeDivideOp(reps:Int) = run(reps)(divideOp)
  def divideOp = get(op)

  def timeDivideSource(reps:Int) = run(reps)(divideSource)
  def divideSource = get(source)
}
