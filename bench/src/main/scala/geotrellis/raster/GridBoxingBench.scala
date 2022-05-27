/*
 * Copyright 2021 Azavea & Astraea, Inc.
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

import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import scala.util.Random

/** See issue https://github.com/locationtech/geotrellis/issues/3427 */
@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class GridBoxingBench {
  val size = 1024

  @Param(Array("short", "int"))
  var cellType: String = _

  var tile: MutableArrayTile = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    tile = cellType match {
      case "short" =>
        ShortArrayTile(
          1 to size * size map (_.toShort) toArray, size, size
        )
      case "int" =>
        IntArrayTile(
          1 to size * size toArray, size, size
        )
    }
  }

  var row: Int = _
  var col: Int = _
  var value: Int = _
  @Setup(Level.Invocation)
  def selectCell(): Unit = {
    row = Random.nextInt(size)
    col = Random.nextInt(size)
    value = Random.nextInt()
  }

  @Benchmark
  def setCell(): Tile = {
    tile.set(row, col, value)
    tile
  }
}
