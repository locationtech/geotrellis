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

package geotrellis

import geotrellis.process._
import geotrellis._
import geotrellis.testkit._

import org.scalatest._

class LiteralSpec extends FunSpec 
                     with Matchers 
                     with TestEngine {
  describe("The Literal operation") {
    it("should work with Int") {
      get(Literal(33)) should be (33)
    }

    it("should work with String") {
      get(Literal("foo")) should be ("foo")
    }
    
    it("should work with List(1,2,3)") {
      get(Literal(List(1,2,3))) should be (List(1,2,3))
    }
  }
}
