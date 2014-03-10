package geotrellis.logic

import geotrellis._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class CollectSpec extends FunSpec 
                     with TestServer
                     with ShouldMatchers {
  describe("Collect") {
    it("should take a Seq of Op[Int]'s and turn it into a Seq[Int]") {
      val seq = Seq(Literal(1),Literal(2),Literal(3))
      val result = get(Collect(seq))
      result should be (Seq(1,2,3))
    }
  }

  describe("CollectMap") {
    it("should take a Map[String,Op[Int]] and turn it into a Map[String,Int]") {
      val map = Map("one" -> Literal(1),"two" -> Literal(2),"three" -> Literal(3))
      val result = get(Collect(map))
      result should be (Map("one" -> 1,"two" -> 2,"three" -> 3))
    }
  }
}
