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

package geotrellis

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class OperationSpec extends FunSpec 
                       with ShouldMatchers 
                       with TestServer {
  describe("Operation.flatten") {
    it("should flatten an Op[Op[Int]] into an Op[Int]") {
      val iOpOp:Op[Op[Int]] = Literal(1).map { i => Literal(i) }
      val i:Op[Int] = iOpOp.flatten
      get(i) should be (1)
    }
  }

  describe("Operation.filter") {
    it("should fail if the op does not meet predicate") {
      val op = 
        for(i <- Literal(1) if i == 2) yield { Literal(i+10) }
      run(op) match {
        case process.Complete(_,_) => withClue("Should have failed") { assert(false) }
        case process.Error(_,_) =>
      }
    }

    it("should succeed if the op does not meet predicate") {
      val op = 
        for(i <- Literal(1) if i == 1) yield { Literal(i+10) }

      run(op) match {
        case process.Complete(_,_) => 
        case process.Error(_,_) => withClue("Should not have failed") { assert(false) }
      } 
    }
  }
}
