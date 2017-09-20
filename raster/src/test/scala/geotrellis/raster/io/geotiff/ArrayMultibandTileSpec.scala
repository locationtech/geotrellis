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

import geotrellis.raster._

import geotrellis.raster.testkit._

import org.scalatest._

class ArrayMultibandTileSpec extends FunSpec with Matchers {

  val mbt1 =
    ArrayMultibandTile(
      ArrayTile(Array.ofDim[Int](15*10).fill(1), 15, 10),
      ArrayTile(Array.ofDim[Int](15*10).fill(2), 15, 10),
      ArrayTile(Array.ofDim[Int](15*10).fill(3), 15, 10))

  val mbt2 =
    ArrayMultibandTile(
      ArrayTile(Array.ofDim[Double](15*10).fill(1.5), 15, 10),
      ArrayTile(Array.ofDim[Double](15*10).fill(2.5), 15, 10),
      ArrayTile(Array.ofDim[Double](15*10).fill(5.0), 15, 10))

  describe("ArrayMultibandTile subset combine methods") {

    it("should work correctly on integer-valued tiles") {
      val actual = mbt1.combine(List(0,1))({ seq: Seq[Int] => seq.sum }).toArray
      val expected = mbt1.band(2).toArray

      (actual.zip(expected)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected")
      })
    }

    it("should work correctly on double-valued tiles") {
      val actual = mbt2.combineDouble(List(0,1))({ seq: Seq[Double] => seq.sum + 1.0 }).toArray
      val expected = mbt2.band(2).toArray

      (actual.zip(expected)).foreach({ pair =>
        assert(pair._1 == pair._2, "actual should equal expected")
      })
    }
  }

  describe("ArrayMultibandTile subset map methods") {

    it("should work correctly on integer-valued tiles") {
      val actual = mbt1.map(List(0,2))({ (band, z) => band + z })
      val expected = ArrayMultibandTile(
        ArrayTile(Array.ofDim[Int](15*10).fill(1), 15, 10),
        ArrayTile(Array.ofDim[Int](15*10).fill(2), 15, 10),
        ArrayTile(Array.ofDim[Int](15*10).fill(5), 15, 10))

      (0 until 3).foreach({ b =>
        val actualArray = actual.band(b).toArray
        val expectedArray = expected.band(b).toArray

        actualArray.zip(expectedArray).foreach({ pair =>
          assert(pair._1 == pair._2, s"actual should equal expected in band $b")
        })
      })
    }

    it("should work correctly on double-valued tiles") {
      val actual = mbt2.mapDouble(List(0,2))({ (band, z) => band + 2.0 * z })
      val expected = ArrayMultibandTile(
        ArrayTile(Array.ofDim[Double](15*10).fill(3.0), 15, 10),
        ArrayTile(Array.ofDim[Double](15*10).fill(2.5), 15, 10),
        ArrayTile(Array.ofDim[Double](15*10).fill(12.0), 15, 10))

      (0 until 3).foreach({ b =>
        val actualArray = actual.band(b).toArray
        val expectedArray = expected.band(b).toArray

        actualArray.zip(expectedArray).foreach({ pair =>
          assert(pair._1 == pair._2, s"actual should equal expected in band $b")
        })
      })
    }
  }

  describe("ArrayMultibandTile - other methods") {
    it("safeBand") {
      mbt1.bandSafe(-1) shouldBe None
      mbt1.bandSafe(0) shouldBe Some(mbt1.band(0))
      mbt1.bandSafe(100) shouldBe None
    }
  }
}
