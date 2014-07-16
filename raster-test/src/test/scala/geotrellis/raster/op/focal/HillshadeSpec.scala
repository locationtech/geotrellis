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

package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.engine._
import geotrellis.feature.Extent
import geotrellis.raster.op._
import geotrellis.raster.render._
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._

class HillshadeSpec extends FunSuite with TestEngine with TileBuilders {

  def grayscale(n: Int) = {
    val ns = (1 to 128).toArray
    val limits = ns.map(i => i * n)
    val colors = ns.map(i => ((i * 65536 * 2 + i * 256 * 2 + i * 2) << 8) | 255)
    ColorBreaks(limits, colors)
  }

  def time() = System.currentTimeMillis()

  // for more information on how hillshade work, see: http://bit.ly/Qj0YPg.
  // note that we scale by 128 not 256, so our result is 77 instead of 154.

  test("esri hillshade") {
    val re = RasterExtent(Extent(0.0, 0.0, 25.0, 25.0), 5.0, 5.0, 5, 5)
    val arr = Array(
      0, 0,    0,    0,    0,
      0, 2450, 2461, 2483, 0,
      0, 2452, 2461, 2483, 0,
      0, 2447, 2455, 2477, 0,
      0, 0,    0,    0,    0)
    val tile = IntArrayTile(arr, 5, 5)

    val cs = CellSize(5.0, 5.0)
    val aspect = get(Aspect(tile, cs))
    val slope = get(Slope(tile, cs, 1.0))

    val h = (aspect, slope).hillshade(315.0, 45.0)
    val h2 = tile.hillshade(cs, 315.0, 45.0, 1.0)

    assert(h.get(2, 2) === 77)
    assert(h2.get(2, 2) === 77)
  }
}
