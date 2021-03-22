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
      1,2,3,4,
      1,1,1,4,
      1,1,1,4,
      1,2,3,4), 4, 4),
    GridBounds(1, 1, 2, 2)
  )

  it("padded + tile => tile") {
    val ans = padded + tile
    ans.get(0,0) shouldBe 3
    ans.dimensions shouldBe Dimensions(2, 2)
    // info("\n" + ans.asciiDraw())
  }

  it("padded + padded => padded") {
    val ans = (padded.combine(padded)(_ + _)).asInstanceOf[BufferTile]

    ans.bufferTop shouldBe 1
    ans.bufferLeft shouldBe 1
    ans.bufferRight shouldBe 1
    ans.bufferBottom shouldBe 1
    ans.dimensions shouldBe Dimensions(2, 2)

    // info("\n" + ans.sourceTile.asciiDraw())
    val ansDouble = (padded.combineDouble(padded)(_ + _)).asInstanceOf[BufferTile]

    ansDouble.bufferTop shouldBe 1
    ansDouble.bufferLeft shouldBe 1
    ansDouble.bufferRight shouldBe 1
    ansDouble.bufferBottom shouldBe 1
    ansDouble.dimensions shouldBe Dimensions(2, 2)
  }

  it("padded + padded => padded (as Tile)") {
    val tile: Tile = padded
    val ans = (tile.combine(tile)(_ + _)).asInstanceOf[BufferTile]

    ans.bufferTop shouldBe 1
    ans.bufferLeft shouldBe 1
    ans.bufferRight shouldBe 1
    ans.bufferBottom shouldBe 1
    ans.dimensions shouldBe Dimensions(2, 2)
    // info("\n" + ans.sourceTile.asciiDraw())

    val ansDouble = (tile.combineDouble(tile)(_ + _)).asInstanceOf[BufferTile]

    ansDouble.bufferTop shouldBe 1
    ansDouble.bufferLeft shouldBe 1
    ansDouble.bufferRight shouldBe 1
    ansDouble.bufferBottom shouldBe 1
    ansDouble.dimensions shouldBe Dimensions(2, 2)
  }

  it("tile center bounds must be contained by underlying tile") {
    BufferTile(tile, GridBounds[Int](0,0,1,1))

    assertThrows[IllegalArgumentException] {
      BufferTile(tile, GridBounds[Int](0,0,2,2))
    }
    assertThrows[IllegalArgumentException] {
      BufferTile(tile, GridBounds[Int](-1,0,1,1))
    }
  }
}