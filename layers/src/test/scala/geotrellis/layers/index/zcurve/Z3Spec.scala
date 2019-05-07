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

import org.scalatest._

class Z3Spec extends FunSpec with Matchers {
  describe("Z3 encoding") {
    it("interlaces bits"){
      // (x,y,z) - x has the lowest sigfig bit
      Z3(1,0,0).z should equal(1)
      Z3(0,1,0).z should equal(2)      
      Z3(0,0,1).z should equal(4)
      Z3(1,1,1).z should equal(7)
    }

    it("deinterlaces bits") {
      Z3(23,13,200).decode  should equal(23, 13, 200)
      
      //only 21 bits are saved, so Int.MaxValue is CHOPPED
      Z3(Int.MaxValue, 0, 0).decode should equal(2097151, 0, 0)
      Z3(Int.MaxValue, 0, Int.MaxValue).decode should equal(2097151, 0, 2097151)
    }

    it("unapply"){
      val Z3(x,y,z) = Z3(3,5,1)
      x should be (3)
      y should be (5)
      z should be (1)
    }    
  }
}
