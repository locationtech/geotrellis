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

package geotrellis.layers.index.hilbert

import geotrellis.layers.index.HilbertKeyIndexMethod
import org.scalatest._

import geotrellis.tiling.{KeyBounds, SpatialKey}

class HilbertSpatialKeyIndexSpec extends FunSpec with Matchers{

  val upperBound: Int = 64

  describe("HilbertSpatialKeyIndex tests"){
    it("Generates a Long index given a SpatialKey"){
      val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(upperBound,upperBound), 6) //what are the SpatialKeys used for?

      val keys = 
        for(col <- 0 until upperBound;
           row <- 0 until upperBound) yield {
          hilbert.toIndex(SpatialKey(col,row))
        }
    
      keys.distinct.size should be (upperBound * upperBound)
      keys.min should be (0)
      keys.max should be (upperBound * upperBound - 1)
    }

    it("generates hand indexes you can hand check 2x2"){
     val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(upperBound,upperBound), 1) 
     //right oriented
     hilbert.toIndex(SpatialKey(0,0)) should be (0)
     hilbert.toIndex(SpatialKey(0,1)) should be (1) 
     hilbert.toIndex(SpatialKey(1,1)) should be (2) 
     hilbert.toIndex(SpatialKey(1,0)) should be (3)
    }

    it("generates hand indexes you can hand check 4x4"){
     val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(upperBound,upperBound), 2) //what are the SpatialKeys used for?
     val grid = List[SpatialKey]( SpatialKey(0,0), SpatialKey(1,0), SpatialKey(1,1), SpatialKey(0,1), 
                                  SpatialKey(0,2), SpatialKey(0,3), SpatialKey(1,3), SpatialKey(1,2), 
                                  SpatialKey(2,2), SpatialKey(2,3), SpatialKey(3,3), SpatialKey(3,2), 
                                  SpatialKey(3,1), SpatialKey(2,1), SpatialKey(2,0), SpatialKey(3,0))
     for(i <- 0 to 15){
       hilbert.toIndex(grid(i)) should be (i)
     }
    }

    it("Generates a Seq[(Long,Long)] given a key range (SpatialKey,SpatialKey)"){
        //hand re-checked examples
        val hilbert = HilbertSpatialKeyIndex(SpatialKey(0,0), SpatialKey(upperBound,upperBound), 2) 

        // single point, min corner
        var idx = hilbert.indexRanges((SpatialKey(0,0), SpatialKey(0,0))) 
        idx.length should be (1)
        idx.toSet should be (Set(0->0))

        // single point, max corner
        idx = hilbert.indexRanges((SpatialKey(3,3), SpatialKey(3,3))) 
        idx.length should be (1)
        idx.toSet should be (Set(10->10)) // aha, not 15 as you might think!

        //square grids
        idx = hilbert.indexRanges((SpatialKey(0,0), SpatialKey(1,1))) 
        idx.length should be (1)
        idx.toSet should be (Set(0->3))

        idx = hilbert.indexRanges((SpatialKey(0,0), SpatialKey(3,3))) 
        idx.length should be (1)
        idx.toSet should be (Set(0->15))

        //check some subgrid
        idx = hilbert.indexRanges((SpatialKey(1,0), SpatialKey(3,2))) 
        idx.length should be (3)
        idx.toSet should be (Set(1->2, 7->8, 11->15))
     }
  }

  it("Generates a Correct index given a 4x4 square on a 10th zoom level") {
    val keyBounds = KeyBounds(SpatialKey(177, 409), SpatialKey(178, 410))
    val index = HilbertKeyIndexMethod.createIndex(keyBounds)
    index.toIndex(SpatialKey(178, 410)) should be (2)
  }
}
