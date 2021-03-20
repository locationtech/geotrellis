/*
 * Copyright 2016 Azavea
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

import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import scala.collection.mutable
import spire.syntax.cfor._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class BufferTileSpec extends AnyFunSpec with Matchers with RasterMatchers with TileBuilders {

  val tile = IntArrayTile(Array(2,2,2,2), 2, 2)
  // center is all 1s, buffer is all 0
  val padded: BufferTile = new BufferTile(
    ArrayTile(Array(
      0,0,0,0,
      0,1,1,0,
      0,1,1,0,
      0,0,0,0), 4, 4),
    GridBounds(1, 1, 2, 2)
  )

  it("padded + tile => tile") {
    val ans = padded + tile
    info("\n" + ans.asciiDraw())
  }

  it("padded + padded => padded") {
    val ans = padded + padded
    info("\n" + ans.asciiDraw())
  }

}