package geotrellis.raster.op.focal

import geotrellis._

import scala.collection.mutable.Set
import scala.math._

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

import Movement._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CursorMaskSpec extends FunSpec with ShouldMatchers {
  val testArray = Array(Array( 1,  2,  3,  4),
                        Array( 5,  6,  7,  8),
                        Array( 9, 10, 11, 12),
                        Array(13, 14, 15, 16))

  describe("CursorMask") {
    it("should mask correctly for center square shape") {
      val mask = TestCursor.maskFromString("""
             X X X X
             X 0 0 X
             X 0 0 X
             X X X X
                                           """)

      val s = Set[Int]()

      for(y <- 0 to 3) {
        mask.foreachX(y) { x => s += testArray(y)(x) }
      }

      s.toSeq.sorted should equal (Seq(6,7,10,11))
    }

    it("should calculate masked values correctly when moving up") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 X 0 0 X
                 X X X X
                 X 0 0 X
                                           """)
      val s = Set[Int]()

      mask.foreachMasked(Up) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(10,11))
    }

    it("should calculated masked values correctly when moving down") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)
      val s = Set[Int]()
      mask.foreachMasked(Down) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(1,2,4,7,12))
    }

    it("should calculate unmasked values correctly when moving up") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 X 0 0 X
                 X X X X
                 X 0 0 X
                                           """)
      val s = Set[Int]()

      mask.foreachUnmasked(Up) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(6,7,14,15))
    }

    it("should calculated unmasked values correctly when moving down") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)
      val s = Set[Int]()

      mask.foreachUnmasked(Down) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,8,11))
    }

    it("should calculate masked values correctly when moving left") {
      val mask = TestCursor.maskFromString("""
                 X 0 X X
                 X 0 X 0
                 X 0 X X
                 X 0 0 X
                                           """)
      val s = Set[Int]()
      mask.foreachMasked(Left) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(3,7,11,16))
    }

    it("should calculated masked values correctly when moving right") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)

      val s = Set[Int]()

      mask.foreachMasked(Right) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(7,9,13,15))
    }

    it("should calculate unmasked values correctly when moving left") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)

      val s = Set[Int]()

      mask.foreachUnmasked(Left) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(8,10,14,16))
    }

    it("should calculated unmasked values correctly when moving right") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 X X 0
                 X 0 0 X
                 X 0 X 0
                                           """)

      val s = Set[Int]()

      mask.foreachUnmasked(Right) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,11,14))
    }

    it("should calculate correct west column for mask") {
      val mask = TestCursor.maskFromString("""
                    0 X 0 X 0
                    0 0 X 0 X
                    0 X X X 0
                    X 0 X 0 0
                    0 X 0 X 0
                                           """)
      val s = Set[Int]()
      
      mask.foreachWestColumn { v => s += v }

      s.toSeq.sorted should equal (Seq(0,1,2,4))
    }

    it("should calculate correct east column for mask") {
      val mask = TestCursor.maskFromString("""
                    0 X 0 X 0
                    0 0 X 0 X
                    0 X X X 0
                    X 0 X 0 0
                    0 X 0 X X
                                           """)
      val s = Set[Int]()
      
      mask.foreachEastColumn { v => s += v }

      s.toSeq.sorted should equal (Seq(0,2,3))
    }
  }
}
