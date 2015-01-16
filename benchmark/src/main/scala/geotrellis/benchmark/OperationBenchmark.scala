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

import geotrellis.raster._
import geotrellis.engine._

import com.google.caliper.Benchmark
import com.google.caliper.Runner
import com.google.caliper.SimpleBenchmark

/**
  * Extend this to create a main object which will run 'cls' (a benchmark).
  */
abstract class BenchmarkRunner(cls: java.lang.Class[_ <: Benchmark]) {
  def main(args: Array[String]): Unit = Runner.main(cls, args: _*)
}

/**
 * Extend this to create an actual benchmarking class.
 */
trait OperationBenchmark extends SimpleBenchmark with GeoTrellisBenchmark
