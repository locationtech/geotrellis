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

package geotrellis.layers.index.zcurve

import geotrellis.layers._
import geotrellis.tiling._

import org.scalatest._

import scala.collection.immutable.TreeSet

class ZSpatialKeyIndexSpec extends FunSpec with Matchers {

  val upperBound = 64
  val keyBounds = KeyBounds(SpatialKey(0, 0), SpatialKey(100, 100))

  describe("ZSpatialKeyIndex test") {
    it("generates an index from a SpatialKey"){

      val zsk = new ZSpatialKeyIndex(keyBounds)

      val keys =
        for(col <- 0 until upperBound;
             row <- 0 until upperBound) yield {
          zsk.toIndex(SpatialKey(col, row))
        }

      keys.distinct.size should be (upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound - 1)
    }

    it("generates indexes you can hand check 2x2"){
     val zsk = new ZSpatialKeyIndex(keyBounds)
     val keys= List[SpatialKey](SpatialKey(0,0),SpatialKey(1,0),
                                SpatialKey(0,1),SpatialKey(1,1))

     for(i <- 0 to 3){
       zsk.toIndex(keys(i)) should be(i)
     }
    }


    it("generates indexes you can hand check 4x4"){
     val zsk = new ZSpatialKeyIndex(keyBounds)
     val keys= List[SpatialKey](SpatialKey(0,0),SpatialKey(1,0),
                                SpatialKey(0,1),SpatialKey(1,1),
                                SpatialKey(2,0),SpatialKey(3,0),
                                SpatialKey(2,1),SpatialKey(3,1),
                                SpatialKey(0,2),SpatialKey(1,2),
                                SpatialKey(0,3),SpatialKey(1,3),
                                SpatialKey(2,2),SpatialKey(3,2),
                                SpatialKey(2,3),SpatialKey(3,3))

     for(i <- 0 to 15){
       zsk.toIndex(keys(i)) should be(i)
     }
    }

    it("generates a Seq[(Long, Long)] from a keyRange (SpatialKey,SpatialKey)"){
     val zsk = new ZSpatialKeyIndex(keyBounds)

     //checked by hand 4x4
     var idx: Seq[(BigInt, BigInt)] = zsk.indexRanges((SpatialKey(0,0), SpatialKey(1,1)))
     idx.length should be(1)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)

     idx = zsk.indexRanges((SpatialKey(0,0), SpatialKey(0,0)))
     idx.length should be(1)
     idx(0)._1 should be(idx(0)._2)
     idx(0)._1 should be(0)

     idx = zsk.indexRanges((SpatialKey(2,0), SpatialKey(3,1)))
     idx.length should be(1)
     idx(0)._1 should be(4)
     idx(0)._2 should be(7)

     idx = zsk.indexRanges((SpatialKey(0,2), SpatialKey(1,3)))
     idx.length should be(1)
     idx(0)._1 should be(8)
     idx(0)._2 should be(11)

     idx = zsk.indexRanges((SpatialKey(2,2), SpatialKey(3,3)))
     idx.length should be(1)
     idx(0)._1 should be(12)
     idx(0)._2 should be(15)

     idx = zsk.indexRanges((SpatialKey(0,0), SpatialKey(3,3)))
     idx.length should be(1)
     idx(0)._1 should be(0)
     idx(0)._2 should be(15)

     //check non-consecutive cases
     idx = zsk.indexRanges((SpatialKey(0,0), SpatialKey(2,1)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(4)
     idx(1)._1 should be (idx(1)._2)
     idx(1)._1 should be(6)


     idx = zsk.indexRanges((SpatialKey(0,0), SpatialKey(1,2)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)
     idx(1)._1 should be(8)
     idx(1)._2 should be(9)

     idx = zsk.indexRanges((SpatialKey(0,0), SpatialKey(1,2)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)
     idx(1)._1 should be(8)
     idx(1)._2 should be(9)

     idx = zsk.indexRanges((SpatialKey(0,0), SpatialKey(1,2)))
     idx.length should be(2)
     idx(0)._1 should be(0)
     idx(0)._2 should be(3)
     idx(1)._1 should be(8)
     idx(1)._2 should be(9)

     idx = zsk.indexRanges((SpatialKey(1,1), SpatialKey(3,2)))
     idx.length should be(4)
     idx(0)._1 should be(3)
     idx(0)._2 should be(3)
     idx(1)._1 should be(6)
     idx(1)._2 should be(7)
     idx(2)._1 should be(9)
     idx(2)._2 should be(9)
     idx(3)._1 should be(12)
     idx(3)._2 should be(13)

     idx = zsk.indexRanges((SpatialKey(2,0), SpatialKey(2,3)))
     idx.length should be(4)
     idx(0)._1 should be(4)
     idx(0)._2 should be(4)
     idx(1)._1 should be(6)
     idx(1)._2 should be(6)
     idx(2)._1 should be(12)
     idx(2)._2 should be(12)
     idx(3)._1 should be(14)
     idx(3)._2 should be(14)
    }
  }
}
