package geotrellis.logic

import geotrellis._

import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class ForEachSpec extends FunSpec 
                     with TestServer
                     with MustMatchers 
                     with ShouldMatchers {
  describe("The ForEach operation") {
    it("should work with Array[Int]") {
      val ns = Array(1, 2, 3, 4, 5)
      val f:Operation[Array[Int]] = ForEach(Literal(ns))((z:Int) => Literal(z + 1))
      val results = get(f)
      results(0) must be === ns(0) + 1
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
