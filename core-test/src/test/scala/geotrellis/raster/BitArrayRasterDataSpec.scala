/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster

import org.scalatest.FunSpec
import org.scalatest.matchers._

class BitArrayRasterDataSpec extends FunSpec with ShouldMatchers {
  describe("BitArrayRasterData.map") {
    it("should map an inverse function correctly.") {
      val arr = Array[Byte](0,1,2,3,4,5,6,7,8)
      val b = BitArrayRasterData(arr,3*8,3)
      val result = b.map(i => i+1)
      for(i <- 0 until b.length) {
        b(i) should not be result(i)
      }
    }

    it("should produce all 1 values for -1 array.") {
      val arr = Array[Byte](0,1,2,3,4,5,6,7,8).map(b => -1.toByte)
      val b = BitArrayRasterData(arr,3*8,3)
      for(col <- 0 until b.cols;
          row <- 0 until b.rows) {
        withClue(s"failed at $col, $row") {
          b.get(col,row) should be (1)
        }
      }
    }
  }
}
