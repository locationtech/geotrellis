package geotrellis.raster.op.focal

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis._
import geotrellis.raster._

import scala.collection.mutable.Set

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class FocalStrategySpec extends FunSpec with ShouldMatchers {
  describe("CursorStrategy") {
    it("should execute the ZigZag traversal strategy correctly") {
      val rex = RasterExtent(Extent(0,0,5,5), 1,1,5,5)
      val d = Array(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25)
      val r = Raster(d,rex)
      val cur = Cursor(r,Square(1))
      
      var lastY = 0
      var lastX = -1

      val calc = new CursorCalculation[Int] {
        def result = 0
        def calc(r:RasterLike,cursor:Cursor) = {
          if(cursor.row != 0 || cursor.col != 0 ) { cursor.isReset should equal(false) }
          if(lastY != cursor.row) {
            cursor.row should be > lastY
            cursor.col should equal(lastX)
            lastY = cursor.row
          } else {
            if(cursor.row % 2 == 0) { (cursor.col - lastX) should equal (1) }
            else { (cursor.col - lastX) should equal (-1) }
          }
          lastX = cursor.col
        }
      }

      CursorStrategy.execute(r,cur,calc, TraversalStrategy.ZigZag,AnalysisArea(r))
    }

    it("should execute the ScanLine traversal strategy correctly") {
      val rex = RasterExtent(Extent(0,0,5,5), 1,1,5,5)
      val d = Array(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25)
      val r = Raster(d,rex)
      val cur = Cursor(r,Square(1))
      
      var lastY = -1
      var lastX = 0

      val calc = new CursorCalculation[Int] {
        def result = 0
        def calc(r:RasterLike,cursor:Cursor) = {
          if(lastY != cursor.row) {
            cursor.isReset should equal(true)
            cursor.row should be > lastY
            cursor.col should equal(0)
            lastY = cursor.row
          } else {
            cursor.col should be  > lastX
          }
          lastX = cursor.col
        }
      }

      CursorStrategy.execute(r,cur,calc,TraversalStrategy.ScanLine,AnalysisArea(r))
    }
  }
}
