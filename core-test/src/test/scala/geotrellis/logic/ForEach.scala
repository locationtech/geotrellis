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

package geotrellis.logic

import geotrellis._

import geotrellis.testkit._

import org.scalatest._

class ForEachSpec extends FunSpec 
                     with TestServer
                     with Matchers {
  describe("The ForEach operation") {
    it("should work with Array[Int]") {
      val ns = Array(1, 2, 3, 4, 5)
      val f:Operation[Array[Int]] = ForEach(Literal(ns))((z:Int) => Literal(z + 1))
      val results = get(f)
      results(0) should be (ns(0) + 1)
    }

    it("should work against two arrays") {
      val a1 = Array("a", "b", "c", "d", "e")
      val a2 = Array("b", "c", "d", "e", "f")
      val results = get(ForEach(a1,a2)(_ + _))
      results should be (Array("ab","bc","cd","de","ef"))
    }

    it("should work against three arrays") {
      val a1 = Array("a", "b", "c", "d", "e")
      val a2 = Array("b", "c", "d", "e", "f")
      val a3 = Array("c", "d", "e", "f", "g")
      val results = get(ForEach(a1,a2,a3)(_ + _ + _))
      results should be (Array("abc","bcd","cde","def","efg"))
    }
  }
}
