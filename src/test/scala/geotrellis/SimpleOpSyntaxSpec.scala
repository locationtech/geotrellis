package geotrellis.foo

import geotrellis._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SimpleOpSyntaxSpec extends FunSpec with MustMatchers with ShouldMatchers {
  val server = TestServer()
  def testOp[T:Manifest](op:Op[T], expected:T){
    server.run(op) must be === expected
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
}
