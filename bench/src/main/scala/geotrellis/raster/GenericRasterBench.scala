/*
 * Copyright 2017 Azavea & Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package geotrellis.raster

import geotrellis.bench.init
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.BenchmarkParams

import scala.util.Random


@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
class GenericRasterBench  {
  import GenericRasterBench._

  @Param(Array( "128", "256", "512", "1024", "2048", "4096", "8192"))
  var size: Int = _

  var tile: Tile = _
  var genericRaster: GRaster[Int] = _

  @Setup(Level.Trial)
  def setup(params: BenchmarkParams): Unit = {
    val len = size * size
    // Because we can end up allocating a lot of memory here,
    // we only initialize the data that is needed for the current benchmark.
    // There may be a better way of doing this using separate `State` classes
    // that are injected on-demand by the framework.
    params.getBenchmark.split('.').last match {
      case "genericRasterMap" ⇒
        genericRaster = new GRaster(init(len)(Random.nextInt))
      case "rasterMap" ⇒
        tile = ArrayTile(init(len)(Random.nextInt), size, size)
      case _ ⇒ throw new MatchError("Have a new benchmark without initialization?")
    }
  }

  @Benchmark
  def genericRasterMap: GRaster[Int] = {
    genericRaster.map { i => i * i }
  }

  @Benchmark
  def rasterMap: Tile = {
    tile.map { i => i * i }
  }
}

object GenericRasterBench {
  class GRaster[T](val array: Array[T]) {
    val size = array.length
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
}
