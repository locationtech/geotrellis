package geotrellis.raster.op.focal

import geotrellis._

import scala.collection.mutable.Set
import scala.math._

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CursorMaskSpec extends FunSpec with ShouldMatchers {
  val testArray = Array(Array( 1,  2,  3,  4),
                        Array( 5,  6,  7,  8),
                        Array( 9, 10, 11, 12),
                        Array(13, 14, 15, 16))
  describe("CursorMask") {
    it("should mask correctly for center square shape") {
      val mask = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 0 || y == 3)
      val s = Set[Int]()

      for(y <- 0 to 3) {
        mask.foreachX(y,0,3) { x => s += testArray(y)(x) }
      }

      s.toSeq.sorted should equal (Seq(6,7,10,11))
    }

    it("should mask edge cases correctly") {
      val mask = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 0 || y == 3)
      val s = Set[Int]()

      // Mask is for cursor on left edge
      for(y <- 0 to 3) {
        mask.foreachX(y,2,1) { x => s += testArray(y)(x) }
      }
      s.toSeq.sorted should equal (Seq(7,11))

      s.clear
      for(y <- 0 to 3) {
        mask.foreachX(y,3,0) { x => s += testArray(y)(x) }
      }
      s.toSeq.sorted should equal (Seq())

      // Mask is for cursor on right edge
      s.clear
      for(y <- 0 to 3) {
        mask.foreachX(y,0,1) { x => s += testArray(y)(x) }
      }
      s.toSeq.sorted should equal (Seq(6,10))

      s.clear
      for(y <- 0 to 3) {
        mask.foreachX(y,0,2) { x => s += testArray(y)(x) }
      }
      s.toSeq.sorted should equal (Seq(6,7,10,11))
    }

    it("should calculate masked values correctly when moving up") {
      /* Mask :  X X X X
       *         X 0 0 X
       *         X X X X
       *         X 0 0 X
       */
      val s = Set[Int]()
      val mask = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 0 || y == 2)

      mask.foreachMaskedUp(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(10,11))

      // left edge
      s.clear
      mask.foreachMaskedUp(2,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(11))

      // right edge
      s.clear
      mask.foreachMaskedUp(0,0,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq())

      s.clear
      mask.foreachMaskedUp(0,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(10))

      /* Mask :  X 0 0 X
       *         X X X X
       *         X X X X
       *         X 0 0 X
       */
      val mask2 = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 1 || y == 2)

      s.clear
      mask2.foreachMaskedUp(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(6,7))

      /* Mask :  X 0 0 X
       *         X X X X
       *         X 0 0 X
       *         X X X X
       */
      val mask3 = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 1 || y == 3)

      // top edge
      s.clear
      mask3.foreachMaskedUp(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(14,15))

      // bottom edge
      s.clear
      mask3.foreachMaskedUp(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(6,7))
    }

    it("should calculated masked values correctly when moving down") {
      /* Mask :  X X X X
       *         0 0 X 0
       *         X 0 0 X
       *         X 0 X 0
       */
      val mask = new CursorMask(4, (x,y) => y == 0 || 
                                           (y == 1 && x == 2) ||
                                           (y == 2 && (x == 0 || x == 3)) || 
                                           (y == 3 && (x == 0 || x == 2)))
      val s = Set[Int]()

      mask.foreachMaskedDown(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(1,2,4,7,12))

      // left edge
      s.clear
      mask.foreachMaskedDown(2,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(4,7,12))

      // right edge
      s.clear
      mask.foreachMaskedDown(0,0,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(1))

      s.clear
      mask.foreachMaskedDown(0,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(1,2))

      // top edge
      s.clear
      mask.foreachMaskedDown(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(12))

      // bottom edge
      s.clear
      mask.foreachMaskedDown(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(1,2,4,7))
    }

    it("should calculate unmasked values correctly when moving up") {
      /* Mask :  X X X X
       *         X 0 0 X
       *         X X X X
       *         X 0 0 X
       */
      val s = Set[Int]()
      val mask = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 0 || y == 2)

      mask.foreachUnmaskedUp(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(6,7,14,15))

      // left edge
      s.clear
      mask.foreachUnmaskedUp(2,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(7,15))

      // right edge
      s.clear
      mask.foreachUnmaskedUp(0,0,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq())

      s.clear
      mask.foreachUnmaskedUp(0,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(6,14))

      // bottom edge
      s.clear
      mask.foreachUnmaskedUp(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(6,7))

      /* Mask :  X 0 0 X
       *         X X X X
       *         X X X X
       *         X 0 0 X
       */
      val mask2 = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 1 || y == 2)

      s.clear
      mask2.foreachUnmaskedUp(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(14,15))

      // top edge 
      s.clear
      mask2.foreachUnmaskedUp(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(14,15))
    }

    it("should calculated unmasked values correctly when moving down") {
      /* Mask :  X X X X
       *         0 0 X 0
       *         X 0 0 X
       *         X 0 X 0
       */
      val mask = new CursorMask(4, (x,y) => y == 0 || 
                                           (y == 1 && x == 2) ||
                                           (y == 2 && (x == 0 || x == 3)) || 
                                           (y == 3 && (x == 0 || x == 2)))
      val s = Set[Int]()

      mask.foreachUnmaskedDown(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,8,11))

      // left edge
      s.clear
      mask.foreachUnmaskedDown(2,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(8,11))

      // right edge
      s.clear
      mask.foreachUnmaskedDown(0,0,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5))

      s.clear
      mask.foreachUnmaskedDown(0,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5))

      // top edge
      s.clear
      mask.foreachUnmaskedDown(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(11))

      // bottom edge
      s.clear
      mask.foreachUnmaskedDown(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,8))
    }

    it("should calculate masked values correctly when moving left") {
      /* Mask :  X 0 X X
       *         X 0 X 0
       *         X 0 X X
       *         X 0 0 X
       */
      val s = Set[Int]()
      val mask = new CursorMask(4, (x,y) => x == 0 || (x == 3 && y != 1) || 
                                      (y == 0 && x != 1) || (y == 2 && x != 1) ||
                                      (y == 1 && x == 2) )
      mask.foreachMaskedLeft(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(3,7,11,16))

      // left edge
      s.clear
      mask.foreachMaskedLeft(2,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(16))

      // right edge
      s.clear
      mask.foreachMaskedLeft(0,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq())

      s.clear
      mask.foreachMaskedLeft(0,2,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(3,7,11))

      // top edge
      s.clear
      mask.foreachMaskedLeft(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(11,16))

      // bottom edge
      s.clear
      mask.foreachMaskedLeft(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(3,7))

      /* Mask :  X 0 0 X
       *         X X X X
       *         X X X X
       *         X 0 0 X
       */
      val mask2 = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 1 || y == 2)

      s.clear
      mask2.foreachMaskedLeft(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(4,16))
    }

    it("should calculated masked values correctly when moving right") {
      /* Mask :  X X X X
       *         0 0 X 0
       *         X 0 0 X
       *         X 0 X 0
       */
      val mask = new CursorMask(4, (x,y) => y == 0 || 
                                           (y == 1 && x == 2) ||
                                           (y == 2 && (x == 0 || x == 3)) || 
                                           (y == 3 && (x == 0 || x == 2)))
      val s = Set[Int]()

      mask.foreachMaskedRight(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(7,9,13,15))

      // left edge
      s.clear
      mask.foreachMaskedRight(2,0,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(7,15))

      // right edge
      s.clear
      mask.foreachMaskedRight(0,0,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(9,13))

      s.clear
      mask.foreachMaskedRight(0,2,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(7,9,13,15))
      
      // top edge
      s.clear
      mask.foreachMaskedRight(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(9,13,15))

      // bottom edge
      s.clear
      mask.foreachMaskedRight(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(7))

    }

    it("should calculate unmasked values correctly when moving left") {
      /* Mask :  X X X X
       *         0 0 X 0
       *         X 0 0 X
       *         X 0 X 0
       */
      val mask = new CursorMask(4, (x,y) => y == 0 || 
                                           (y == 1 && x == 2) ||
                                           (y == 2 && (x == 0 || x == 3)) || 
                                           (y == 3 && (x == 0 || x == 2)))
      val s = Set[Int]()

      mask.foreachUnmaskedLeft(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(8,10,14,16))

      // left edge
      s.clear
      mask.foreachUnmaskedLeft(2,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(8,16))

      // right edge
      s.clear
      mask.foreachUnmaskedLeft(0,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(10,14))

      // top edge
      s.clear
      mask.foreachUnmaskedLeft(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal(Seq(10,14,16))

      // bottom edge
      s.clear
      mask.foreachUnmaskedLeft(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal(Seq(8))

      /* Mask :  X 0 0 X
       *         X X X X
       *         X X X X
       *         X 0 0 X
       */
      val mask2 = new CursorMask(4, (x,y) => x == 0 || x == 3 || y == 1 || y == 2)

      s.clear
      mask2.foreachUnmaskedLeft(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(2,14))
    }

    it("should calculated unmasked values correctly when moving right") {
      /* Mask :  X X X X
       *         0 X X 0
       *         X 0 0 X
       *         X 0 X 0
       */
      val mask = new CursorMask(4, (x,y) => y == 0 || 
                                           (y == 1 && (x == 2 || x == 1)) ||
                                           (y == 2 && (x == 0 || x == 3)) || 
                                           (y == 3 && (x == 0 || x == 2)))
      val s = Set[Int]()

      mask.foreachUnmaskedRight(0,3,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,11,14))

      // left edge
      s.clear
      mask.foreachUnmaskedRight(2,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(11))

      // right edge
      s.clear
      mask.foreachUnmaskedRight(0,0,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5))

      s.clear
      mask.foreachUnmaskedRight(0,1,0,3) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,14))

      // top edge
      s.clear
      mask.foreachUnmaskedRight(0,3,2,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(11,14))

      // bottom edge
      s.clear
      mask.foreachUnmaskedRight(0,3,0,1) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5))
    }
  }
}
