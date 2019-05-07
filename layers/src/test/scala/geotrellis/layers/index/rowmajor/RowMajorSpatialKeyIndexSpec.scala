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

package geotrellis.layers.index.rowmajor

import scala.collection.immutable.TreeSet
import org.scalatest._
import geotrellis.layers.index.KeyIndex
import geotrellis.tiling.{SpatialKey, KeyBounds}

class RowMajorSpatialKeyIndexSpec extends FunSpec with Matchers{

  val upperBound: Int = 64

  describe("RowMajorSpatialKeyIndex tests") {

    it("Generates a Long index given a SpatialKey"){

      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(upperBound-1, upperBound-1)))
   
      val keys = 
        for(col <- 0 until upperBound;
             row <- 0 until upperBound) yield {
          rmajor.toIndex(SpatialKey(col, row))
        }
   
      keys.distinct.size should be (upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound - 1)
    }
      
      
    it("generates indexes you can check by hand 2x2"){
      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(1,1))) 
      val grid = List[SpatialKey](SpatialKey(0,0), SpatialKey(1,0), SpatialKey(0,1), SpatialKey(1,1)) 
       rmajor.toIndex(grid(0)) should be(0)
       rmajor.toIndex(grid(1)) should be(1)
       rmajor.toIndex(grid(2)) should be(2)
       rmajor.toIndex(grid(3)) should be(3)
    }

    it("generates indexes you can check by hand 4x4"){
      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(3,3))) 
      var grid = List[SpatialKey]()
      for{ i <- 0 to 3; j <- 0 to 3} 
        rmajor.toIndex(SpatialKey(j,i)) should be (4*i+j)
    }

    it("Generates a Seq[(Long,Long)] given a key range (SpatialKey,SpatialKey)"){
      val rmajor = new RowMajorSpatialKeyIndex(KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(3,3))) 

      //checked by hand 4x4

      //cols
      var idx = rmajor.indexRanges((SpatialKey(1,0), SpatialKey(1,3)))
      idx.length should be(4)
      idx(0)._1 should be (1)
      idx(0)._2 should be (1)
      idx(1)._1 should be (5)
      idx(1)._2 should be (5)
      idx(2)._1 should be (9)
      idx(2)._2 should be (9)
      idx(3)._1 should be (13)
      idx(3)._2 should be (13)

      idx = rmajor.indexRanges((SpatialKey(0,0), SpatialKey(0,3)))
      idx.length should be(4)
      idx(0)._1 should be (0)
      idx(0)._2 should be (0)
      idx(1)._1 should be (4)
      idx(1)._2 should be (4)
      idx(2)._1 should be (8)
      idx(2)._2 should be (8)
      idx(3)._1 should be (12)
      idx(3)._2 should be (12)

      idx = rmajor.indexRanges((SpatialKey(2,0), SpatialKey(2,3)))
      idx.length should be(4)
      idx(0)._1 should be (2)
      idx(0)._2 should be (2)
      idx(1)._1 should be (6)
      idx(1)._2 should be (6)
      idx(2)._1 should be (10)
      idx(2)._2 should be (10)
      idx(3)._1 should be (14)
      idx(3)._2 should be (14)

      idx = rmajor.indexRanges((SpatialKey(3,0), SpatialKey(3,3)))
      idx.length should be(4)
      idx(0)._1 should be (3)
      idx(0)._2 should be (3)
      idx(1)._1 should be (7)
      idx(1)._2 should be (7)
      idx(2)._1 should be (11)
      idx(2)._2 should be (11)
      idx(3)._1 should be (15)
      idx(3)._2 should be (15)

      //rows
      idx = rmajor.indexRanges((SpatialKey(0,0), SpatialKey(3,0)))
      idx.length should be(1)
      idx(0)._1 should be (0)
      idx(0)._2 should be (3)

      idx = rmajor.indexRanges((SpatialKey(0,1), SpatialKey(3,1)))
      idx.length should be(1)
      idx(0)._1 should be (4)
      idx(0)._2 should be (7)

      idx = rmajor.indexRanges((SpatialKey(0,2), SpatialKey(3,2)))
      idx.length should be(1)
      idx(0)._1 should be (8)
      idx(0)._2 should be (11)

      idx = rmajor.indexRanges((SpatialKey(0,3), SpatialKey(3,3)))
      idx.length should be(1)
      idx(0)._1 should be (12)
      idx(0)._2 should be (15)

      //subgrids
      idx = rmajor.indexRanges((SpatialKey(0,0), SpatialKey(2,1)))
      idx.length should be(2)
      idx(0)._1 should be (0)
      idx(0)._2 should be (2)
      idx(1)._1 should be (4)
      idx(1)._2 should be (6)

      idx = rmajor.indexRanges((SpatialKey(0,0), SpatialKey(1,2)))
      idx.length should be(3)
      idx(0)._1 should be (0)
      idx(0)._2 should be (1)
      idx(1)._1 should be (4)
      idx(1)._2 should be (5)
      idx(2)._1 should be (8)
      idx(2)._2 should be (9)

      idx = rmajor.indexRanges((SpatialKey(1,0), SpatialKey(2,1)))
      idx.length should be(2)
      idx(0)._1 should be (1)
      idx(0)._2 should be (2)
      idx(1)._1 should be (5)
      idx(1)._2 should be (6)
    }
  }
}
