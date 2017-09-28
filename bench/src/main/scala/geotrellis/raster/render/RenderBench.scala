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
package geotrellis.raster.render

import org.openjdk.jmh.annotations._
import geotrellis.raster._
import geotrellis.raster.render.png._

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
class RenderBench {
  val size = 2560 / 2
  val cmapSize = 50

  var tile: Tile = _
  var cmap: ColorMap = _
  var colorEncoding: PngColorEncoding = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    tile = IntArrayTile(1 to size * size toArray, size, size)
    val step = (size * size) / cmapSize
    val colors = Array(0x0000FF, 0x0080FF, 0x00FF80, 0xFFFF00, 0xFF8000)
    val map =
      (for(x <- 0 until cmapSize) yield {
        x * step -> colors(x % colors.length)
      }).toMap
    cmap =
      ColorMap(map)

    colorEncoding =
      PngColorEncoding(cmap.colors, cmap.options.noDataColor, cmap.options.fallbackColor)
  }

  @Benchmark
  def rendering: Tile = cmap.render(tile)

  @Benchmark
  def pngEncoding: Array[Byte] = PngEncoder(Settings(colorEncoding, PaethFilter)).writeByteArray(tile)

  @Benchmark
  def renderingAndPngEncoding: Array[Byte] = {
    val r2 = cmap.render(tile)
    PngEncoder(Settings(colorEncoding, PaethFilter)).writeByteArray(r2)
  }
}
