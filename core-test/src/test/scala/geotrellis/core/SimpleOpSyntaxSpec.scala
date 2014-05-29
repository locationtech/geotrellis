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

package geotrellis.core

import geotrellis._
import geotrellis.process._
import geotrellis.testkit._

import org.scalatest._

class SimpleOpSyntaxSpec extends FunSpec 
                            with MustMatchers 
                            with Matchers 
                            with TestServer {
  def testOp[T:Manifest](op:Op[T], expected:T){
    get(op) must be === expected
  }
  
  val addOne = (x:Int) => x + 1
  
  describe("The simple op syntax") {
    it("should accept 1 argument functions that return literal values") {
      val plusOne = op { (i:Int) => i + 1 }
      testOp(plusOne(1), 2)
      
      val alternatePlusOne = op(addOne)
      testOp(alternatePlusOne(1),2)
    }

    it("should accept 1 argument functions that return operations") {
      val plusOne = op { (i:Int) => i + 1 }
      
      val plusTwo = op {(i:Int) => plusOne(i + 1)}
      testOp(plusTwo(1), 3)
    }
    
    it("should accept 1 argument functions that return StepResult") {
      val plusOneResult = op {(i:Int) => Result(i + 1)}
      testOp(plusOneResult(1), 2)
    }
    
    it("should accept 2 argument functions that return literal values") {
      val sumOp = op { (a:Int, b:Int) => a + b }
      testOp(sumOp(1,2), 3)
    }

    it("should accept 2 argument functions that return operations") {
      val sumOp = op { (a:Int, b:Int) => a + b }      
      val sumPlusOneOp = op {(a:Int, b:Int) => sumOp(a + 1, b)}
      testOp(sumPlusOneOp(1,2), 4)
    }
    
    it("should accept 2 argument functions that return StepResult") {
      val sumOpResult = op {(a:Int, b:Int) => Result(a + b)}
      testOp(sumOpResult(1,2), 3)
    }

    it("should accept 3 argument functions that return literal values") {
      val sumOp = op { (a:Int, b:Int, c:Int) => a + b + c }
      testOp(sumOp(1,2,3), 6)
    }

    it("should accept 3 argument functions that return operations") {
      val sumOp = op { (a:Int, b:Int, c:Int) => a + b + c }
      val sumPlusOneOp = op {(a:Int, b:Int, c:Int) => sumOp(a + 1, b, c)}
      testOp(sumPlusOneOp(1,2,3), 7)
    }
    
    it("should accept 3 argument functions that return StepResult") {
      val sumOpResult = op { (a:Int, b:Int, c:Int) => Result(a + b + c) }
      testOp(sumOpResult(1,2,3), 6)
    }
    
    it("should accept 4 argument functions that return literal values") {
      val sumOp = op { (a:Int, b:Int, c:Int, d:Int) => a + b + c + d}
      testOp(sumOp(1,2,3,4), 10)
    }

    it("should accept 4 argument functions that return operations") {
      val sumOp = op { (a:Int, b:Int, c:Int, d:Int) => a + b + c + d}
      val sumPlusOneOp = op {(a:Int, b:Int, c:Int, d:Int) => sumOp(a + 1, b, c, d)}
      testOp(sumPlusOneOp(1,2,3,4), 11)
    }
    
    it("should accept 4 argument functions that return StepResult") {
      val sumOpResult = op { (a:Int, b:Int, c:Int, d:Int) => Result(a + b + c + d)}
      testOp(sumOpResult(1,2,3,4), 10)
    }

  }
 
  describe("Operation flatMap") {
    it("should compound operations") {
    	val AddOneOp = op { (x:Int) => x + 1 }
    	val addThree = AddOneOp(1).flatMap( (y:Int) => y + 2 )
        testOp(addThree,4)
    }
    it("is one way to define multi-step operations") {      
      val addOneOp = op { (x:Int) => x + 1 }
      val x = addOneOp(2).flatMap( (a:Int) => addOneOp(a + 3) ).flatMap( (b:Int) => b + 4 )
      testOp(x,11)
      
      val y = addOneOp(2).withResult( (a:Int) => addOneOp(a + 3) ).withResult( (b:Int) => b + 4 )
      testOp(x,11)

    }
    it("can be used with for comprehensions") {
      val la = for (x <- Literal(5); 
                    y <- Literal(x + 1); 
                    z <- Literal(y + 3)
                   ) yield z - 2
      testOp(la, 7)
    }
  }
}
