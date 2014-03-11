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
import geotrellis.process._
import geotrellis.source._

import com.google.caliper.Benchmark
import com.google.caliper.Runner 
import com.google.caliper.SimpleBenchmark

/**
 * Extend this to create a main object which will run 'cls' (a benchmark).
 */
abstract class BenchmarkRunner(cls:java.lang.Class[_ <: Benchmark]) {
  def main(args:Array[String]): Unit = Runner.main(cls, args:_*)
}

/**
 * Extend this to create an actual benchmarking class.
 */
trait OperationBenchmark extends SimpleBenchmark {
  def getRasterExtent(name:String, w:Int, h:Int):RasterExtent = {
    val ext = RasterSource(name).info.get.rasterExtent.extent
    RasterExtent(ext,w,h)
  }
  /**
   * Loads a given raster with a particular height/width.
   */
  def loadRaster(name:String, w:Int, h:Int):Raster =
    RasterSource(name,getRasterExtent(name,w,h)).get

  def get[T](op:Op[T]):T = GeoTrellis.get(op)
  def get[T](source:DataSource[_,T]):T = GeoTrellis.get(source)

  /**
   * Sugar for building arrays using a per-cell init function.
   */
  def init[A:Manifest](size:Int)(init: => A) = {
    val data = Array.ofDim[A](size)
    for (i <- 0 until size) data(i) = init
    data
  }

  /**
   * Sugar to run 'f' for 'reps' number of times.
   */
  def run(reps:Int)(f: => Unit) = {
    var i = 0
    while (i < reps) { f; i += 1 }
  }
}
