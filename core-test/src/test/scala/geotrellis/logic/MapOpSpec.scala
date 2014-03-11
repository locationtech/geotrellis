/***
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
 ***/

package geotrellis.logic

import geotrellis._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class MapOpSpec extends FunSpec 
                   with TestServer
                   with ShouldMatchers {
  describe("MapOp") {
    it("should call a function with no arguments") {
      var called = false
      val result = get(MapOp({ () => called = true }))
      called should be (true)
    }

    it("should call a function with one argument") {
      var v = 0
      val result = get(MapOp(Literal(1))({ i => v = i; v }))
      v should be (1)
      result should be (1)
    }

    it("should call a function with two argument") {
      val result = get(MapOp(Literal(1),Literal(2))(_ + _))
      result should be (3)
    }
  }
}
