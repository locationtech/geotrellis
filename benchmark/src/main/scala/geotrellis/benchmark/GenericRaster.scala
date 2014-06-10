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

import geotrellis.feature._
import geotrellis.raster._
import geotrellis.raster.op.local._

import scala.util.Random

import com.google.caliper.Param

class GRaster[T](val array: Array[T]) {
  val size = array.size
  val newArr = array.clone
  def map(f: T=>T) = {
    var i = 0
    while(i < size) {
      newArr(i) = f(array(i))
      i += 1
    }
    new GRaster(newArr)
  }
}

object GenericRaster extends BenchmarkRunner(classOf[GenericRaster])
class GenericRaster extends OperationBenchmark {
  @Param(Array("128", "256", "512", "1024", "2048", "4096", "8192"))
  var size: Int = 0

  var tile: Tile = null
  var genericRaster: GRaster[Int] = null

  override def setUp() {
    val len = size * size
    tile = ArrayTile(init(len)(Random.nextInt), size, size)
    genericRaster = new GRaster(init(len)(Random.nextInt))

  }

  def timeGenericRasterMap(reps: Int) = run(reps)(genericRasterMap)
  def genericRasterMap = genericRaster.map { i => i * i }

  def timeRasterMap(reps: Int) = run(reps)(rasterMap)
  def rasterMap = tile.map { i => i * i }
}
