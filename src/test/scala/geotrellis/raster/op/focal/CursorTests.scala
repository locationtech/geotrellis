package geotrellis.raster.op.focal

import geotrellis._

import scala.collection.mutable.Set
import scala.math._

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CursorSpec extends FunSpec with ShouldMatchers {
  def createRaster:Raster = {
    val arr = (for(i <- 1 to 100) yield i).toArray
    Raster(arr, RasterExtent(Extent(0,0,10,10),1,1,10,10))
  }

  def createOnesRaster:Raster = {
    val arr = (for(i <- 1 to 100) yield 1).toArray
    Raster(arr, RasterExtent(Extent(0,0,10,10),1,1,10,10))
  }

  describe("Cursor") {
    it("should get all values for middle cursor") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(5,5)
      cursor.getAll.sorted should equal (Seq(45,46,47,55,56,57,65,66,67))
    }

    it("should get all values for corner cursors") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(0,0)
      cursor.getAll.sorted should equal (Seq(1,2,11,12))
      cursor.centerOn(9,0)
      cursor.getAll.sorted should equal (Seq(9,10,19,20))
      cursor.centerOn(0,9)
      cursor.getAll.sorted should equal (Seq(81,82,91,92))
      cursor.centerOn(9,9)
      cursor.getAll.sorted should equal (Seq(89,90,99,100))
     }

    it("should get all values for edge cursors") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(5,0)
      cursor.getAll.sorted should equal (Seq(5,6,7,15,16,17))
      cursor.centerOn(0,5)
      cursor.getAll.sorted should equal (Seq(41,42,51,52,61,62))
      cursor.centerOn(9,5)
      cursor.getAll.sorted should equal (Seq(49,50,59,60,69,70))
      cursor.centerOn(5,9)
      cursor.getAll.sorted should equal (Seq(85,86,87,95,96,97))
     }

    it("should fold left on corners") {
      val r = createOnesRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(0,0)
      cursor.foldLeft(0) { (a,b) => a + b } should be (4)
      cursor.centerOn(9,0)
      cursor.foldLeft(0) { (a,b) => a + b } should be (4)
      cursor.centerOn(0,9)
      cursor.foldLeft(0) { (a,b) => a + b } should be (4)
      cursor.centerOn(9,9)
      cursor.foldLeft(0) { (a,b) => a + b } should be (4)
    }

    it("should fold left on edges") {
      val r = createOnesRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(5,0)
      cursor.foldLeft(0) { (a,b) => a + b } should be (6)
      cursor.centerOn(0,5)
      cursor.foldLeft(0) { (a,b) => a + b } should be (6)
      cursor.centerOn(9,5)
      cursor.foldLeft(0) { (a,b) => a + b } should be (6)
      cursor.centerOn(5,9)
      cursor.foldLeft(0) { (a,b) => a + b } should be (6)
    }
    
    it("should fold left for middle cursor") {
      val r = createOnesRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(5,5)
      cursor.foldLeft(0) { (a,b) => a + b } should be (9)
    }

    it("should track new cells added to cursor during a move to the right") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,1)
      cursor.moveRight()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq(4,14,24))
    }

    it("should track new cells added to the cursor during a move to the left") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(2,1)
      cursor.moveLeft()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq(1,11,21))
    }

    it("should track new cells added to the cursor during a move down") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,1)
      cursor.moveDown()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq(31,32,33))
    }

    it("should track new cells added to the cursor during a move up") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,2)
      cursor.moveUp()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq(1,2,3))
    }

    it("should be able to handle left border cases for tracking new cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,1)
      cursor.moveLeft()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should be able to handle right border cases for tracking new cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(8,0)
      cursor.moveRight()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should be able to handle top border cases for tracking new cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(0,1)
      cursor.moveUp()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should be able to handle bottom border cases for tracking new cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,8)
      cursor.moveDown()
      val s = Set[Int]()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should track old cells added to cursor during a move to the right") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,1)
      cursor.moveRight()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq(1,11,21))
    }

    it("should track old cells added to the cursor during a move to the left") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(2,1)
      cursor.moveLeft()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq(4,14,24))
    }

    it("should track old cells added to the cursor during a move down") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,1)
      cursor.moveDown()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq(1,2,3))
    }

    it("should track old cells added to the cursor during a move up") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,2)
      cursor.moveUp()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq(31,32,33))
    }

    it("should be able to handle left border cases for tracking old cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(0,4)
      cursor.moveRight()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should be able to handle right border cases for tracking old cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1) 
      cursor.centerOn(9,0)
      cursor.moveLeft()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should be able to handle top border cases for tracking old cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,0)
      cursor.moveDown()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should be able to handle bottom border cases for tracking old cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.centerOn(1,9)
      cursor.moveUp()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq())
    }

    it("should be able to mask a corner for getAll") {
      var r = createRaster
      var cursor = new IntCursor(r,1)
      cursor.setMask { (x,y) => x == 0 || y == 0 }
      cursor.centerOn(1,1)
      cursor.getAll.sorted should equal(Seq(12,13,22,23))
      cursor.centerOn(9,9)
      cursor.getAll.sorted should equal(Seq(100))
      cursor.centerOn(5,9) 
      cursor.getAll.sorted should equal(Seq(96,97))
      cursor.centerOn(9,5)
      cursor.getAll.sorted should equal(Seq(60,70))
    }

    it("should be able to mask a corner for tracking new cells") {
      val s = Set[Int]()

      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.setMask { (x,y) => x == 0 || y == 0 }
      cursor.centerOn(1,1)
      cursor.moveDown()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq(32,33))

      s.clear
      cursor.centerOn(9,9)
      cursor.moveUp()
      cursor.foreachNew { x => s += x }
      s.toSeq.sorted should equal (Seq(90))
    }

    it("should be able to mask a corner for tracking old cells") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.setMask { (x,y) => x == 0 || y == 0 }
      cursor.centerOn(8,5)
      cursor.moveUp()
      val s = Set[Int]()
      cursor.foreachOld { x => s += x }
      s.toSeq.sorted should equal (Seq(69,70))
    }

    it("should be able to mask triangle and foldLeft with sum") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.setMask { (x,y) => x+y > 2 }
      cursor.centerOn(2,2)
      // 12+13+14+22+23+32
      cursor.foldLeft(0) { (a,b) => a + b } should be (116)
    }

    it("should be able to mask circle and foldLeft with min") {
      val r = createRaster
      val n = Circle2(3)
      val cursor = Cursor.getInt(r,n)
      cursor.centerOn(4,4)
      cursor.foldLeft(Int.MaxValue) { (a,b) => min(a,b) } should be (15)
    }
    
    it("should be able to mask circle and foldLeft with min for edge case") {
      val r = createRaster
      val n = Circle2(3)
      val cursor = Cursor.getInt(r,n)
      cursor.centerOn(9,9)
      cursor.foldLeft(Int.MaxValue) { (a,b) => min(a,b) } should be (70)
    }

    it("should be able to mask triangle and calculate sum using new and old values when moving right.") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.setMask { (x,y) => x+y > 2 }
      cursor.centerOn(2,2)
      
      val s = Set[Int]()
      cursor.foreach { x => s += x }
      s.toSeq.sorted should equal (Seq(12,13,14,22,23,32))

      cursor.moveRight()
      val news = Set[Int]()
      val olds = Set[Int]()
      cursor.foreachNew { x => news += x }
      cursor.foreachOld { x => olds += x }

      news.toSeq.sorted should equal (Seq(15,24,33))
      olds.toSeq.sorted should equal (Seq(12,22,32))
    }

    it("should be able to mask triangle and calculate sum using new and old values when moving left.") {
      val r = createRaster
      val cursor = new IntCursor(r,1)
      cursor.setMask { (x,y) => x+y > 2 }
      cursor.centerOn(2,2)
      
      val s = Set[Int]()
      cursor.foreach { x => s += x }
      s.toSeq.sorted should equal (Seq(12,13,14,22,23,32))

      cursor.moveLeft()
      val news = Set[Int]()
      val olds = Set[Int]()
      cursor.foreachNew { x => news += x }
      cursor.foreachOld { x => olds += x }

      news.toSeq.sorted should equal (Seq(11,21,31))
      olds.toSeq.sorted should equal (Seq(14,23,32))
    }

  }
}
