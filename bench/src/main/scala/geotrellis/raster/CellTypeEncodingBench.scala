/*
 * Copyright 2020 Azavea
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

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class CellTypeEncodingBench {

  @Param(Array("int16ud44", "uint8raw", "float32"))
  var cellTypeName: String = _
  var cellType: CellType = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    cellType = CellType.fromName(cellTypeName)
  }

  @Benchmark
  def asString(): String = {
    cellType.toString
  }

  @Benchmark
  def fromString(): CellType = {
    CellType.fromName(cellTypeName)
  }
}
