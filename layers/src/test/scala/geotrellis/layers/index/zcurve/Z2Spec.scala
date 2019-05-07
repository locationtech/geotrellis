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

class Z2Spec extends FunSpec with Matchers {
  describe("Z2 encoding") {
    it("interlaces bits"){
      Z2(1,0).z should equal(1)
      Z2(2,0).z should equal(4)
      Z2(3,0).z should equal(5)      
      Z2(0,1).z should equal(2)
      Z2(0,2).z should equal(8)
      Z2(0,3).z should equal(10)      

    }

    it("deinterlaces bits") {
      Z2(23,13).decode  should equal(23, 13)
      Z2(Int.MaxValue, 0).decode should equal(Int.MaxValue, 0)
      Z2(0, Int.MaxValue).decode should equal(0, Int.MaxValue)
      Z2(Int.MaxValue, Int.MaxValue).decode should equal(Int.MaxValue, Int.MaxValue)
    }

    it("unapply"){
      val Z2(x,y) = Z2(3,5)
      x should be (3)
      y should be (5)
    }    

    it("replaces example in Tropf, Herzog paper"){
      // Herzog example inverts x and y, with x getting higher sigfigs
      val rmin = Z2(5,3)
      val rmax = Z2(10,5)
      val p = Z2(4, 7)

      rmin.z should equal (27)
      rmax.z should equal (102)
      p.z should equal (58)

      val (litmax, bigmin) = Z2.zdivide(p, rmin, rmax)   

      litmax.z should equal (55)
      bigmin.z should equal (74)
    }

    it("replicates the wikipedia example") {
      val rmin = Z2(2,2)
      val rmax = Z2(3,6)
      val p = Z2(5, 1)

      rmin.z should equal (12)
      rmax.z should equal (45)
      p.z should equal (19)

      val (litmax, bigmin) = Z2.zdivide(p, rmin, rmax)   

      litmax.z should equal (15)
      bigmin.z should equal (36)
    }

  }
}
