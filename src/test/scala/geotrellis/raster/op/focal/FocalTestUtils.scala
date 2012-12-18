package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.process._
import geotrellis.raster.op._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

case class SeqTestSetup[@specialized(Int,Double)D](adds:Seq[Int],removes:Seq[Int],result:D)

case class CursorSetup(r:Raster,calc:CursorCalculation[Raster],cursor:Cursor) {
  def result(x:Int,y:Int) = {
    cursor.centerOn(x,y)
    calc.calc(r,cursor)
    calc.result.get(x,y)
  }
}

object MockCursor {
  def fromAll(s:Int*) = {
    new MockCursor(s,Seq[Int](),Seq[Int]())
  }

  def fromAddRemove(a:Seq[Int],r:Seq[Int]) = {
    new MockCursor(Seq[Int](),a,r)
  }
}

case class MockCursor(all:Seq[Int],added:Seq[Int],removed:Seq[Int]) extends Cursor(Int.MaxValue,Int.MaxValue,1) {
  centerOn(0,0)

  override val allCells = new CellSet {
    def foreach(f:(Int,Int)=>Unit) = { 
      var i = 0
      for(x <- all) {
        f(i,0)
        i += 1
      } 
    }
  }

  override val addedCells = new CellSet {
    def foreach(f:(Int,Int)=>Unit) = { 
      var i = 0
      for(x <- added) {
        f(i,1)
        i += 1
      } 
    }

  }

  override val removedCells = new CellSet {
    def foreach(f:(Int,Int)=>Unit) = { 
      var i = 0
      for(x <- removed) {
        f(i,2)
        i += 1
      } 
    }
  }

  def raster = {
    val cols = max(all.length,max(added.length,removed.length))
    val data = Array.ofDim[Int](cols,3)
    var i = 0
    val c = all ++ { for(x <- 0 until (cols - all.length)) yield 0 }
    val a = added ++ { for(x <- 0 until (cols - added.length)) yield 0 }
    val r = removed ++ { for(x <- 0 until (cols - removed.length)) yield 0 }
    val d = (c ++ a ++ r).toArray
    Raster(d,RasterExtent(Extent(0,0,cols,3),1,1,cols,3))
  }
}

trait FocalOpSpec extends RasterBuilders with ShouldMatchers {
  def getSetup[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T,r:Raster,n:Neighborhood) = {
    val op = createOp(r,n)
    val calc = op.getCalculation(r,n).asInstanceOf[CursorCalculation[Raster] with Initialization]
    calc.init(r)
    CursorSetup(r,calc,Cursor(r,n))
  }

  def getCursorResult[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T,n:Neighborhood,cursor:MockCursor):Int = {
    val r = cursor.raster
    val op = createOp(r,n)
    val calc = op.getCalculation(r,n).asInstanceOf[CursorCalculation[Raster] with Initialization]
    calc.init(r)
    calc.calc(r,cursor)
    calc.result.get(0,0)
  }

  def getDoubleCursorResult[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T,n:Neighborhood,cursor:MockCursor):Double = {
    val r = cursor.raster
    val op = createOp(r,n)
    val calc = op.getCalculation(r,n).asInstanceOf[CursorCalculation[Raster] with Initialization]
    calc.init(r)
    calc.calc(r,cursor)
    calc.result.getDouble(0,0)
  }

  def testCursorSequence[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T,n:Neighborhood,
                                               setups:Seq[SeqTestSetup[Int]]) = {
    val op = createOp(null,n)
    val calc = op.getCalculation(null,n).asInstanceOf[CursorCalculation[Raster] with Initialization]
    
    var init = true
    for(setup <- setups) {
      val mockCursor = MockCursor.fromAddRemove(setup.adds,setup.removes)
      if(init) { calc.init(mockCursor.raster) ; init = false }
      calc.calc(mockCursor.raster,mockCursor)
      calc.result.get(0,0) should equal(setup.result)
    }
  }

  def testCellwiseSequence[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T,n:Neighborhood,
                                               setups:Seq[SeqTestSetup[Int]]) = {
    val op = createOp(null,n)
    val calc = op.getCalculation(null,n).asInstanceOf[CellwiseCalculation[Raster] with Initialization]
    
    var init = true
    for(setup <- setups) {
      val r = MockCursor.fromAddRemove(setup.adds,setup.removes).raster
      if(init) { calc.init(r) ; init = false }
      var i = 0
      for(x <- setup.adds) {
        calc.add(r,i,1)
        i += 1
      }
      i = 0
      for(x <- setup.removes) {
        calc.remove(r,i,2)
        i += 1
      }
      calc.setValue(0,0)
      calc.result.get(0,0) should equal (setup.result)
    }
  }

  def testDoubleCursorSequence[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T,n:Neighborhood,
                                               setups:Seq[SeqTestSetup[Double]]) = {
    val op = createOp(null,n)
    val calc = op.getCalculation(null,n).asInstanceOf[CursorCalculation[Raster] with Initialization]
    
    var init = true
    for(setup <- setups) {
      val mockCursor = MockCursor.fromAddRemove(setup.adds,setup.removes)
      if(init) { calc.init(mockCursor.raster) ; init = false }
      calc.calc(mockCursor.raster,mockCursor)
      calc.result.getDouble(0,0) should equal(setup.result)
    }
  }

  def testDoubleCellwiseSequence[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T,n:Neighborhood,
                                               setups:Seq[SeqTestSetup[Double]]) = {
    val op = createOp(null,n)
    val calc = op.getCalculation(null,n).asInstanceOf[CellwiseCalculation[Raster] with Initialization]
    
    var init = true
    for(setup <- setups) {
      val r = MockCursor.fromAddRemove(setup.adds,setup.removes).raster
      if(init) { calc.init(r) ; init = false }
      var i = 0
      for(x <- setup.adds) {
        calc.add(r,i,1)
        i += 1
      }
      i = 0
      for(x <- setup.removes) {
        calc.remove(r,i,2)
        i += 1
      }
      calc.setValue(0,0)
      calc.result.getDouble(0,0) should equal (setup.result)
    }
  }


  def getCellwiseResult[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T, n:Neighborhood,
                                              added:Seq[Int],removed:Seq[Int]) = {
    val r = MockCursor.fromAddRemove(added,removed).raster
    val op = createOp(r,n)
    val calc = op.getCalculation(r,n).asInstanceOf[CellwiseCalculation[Raster] with Initialization]
    calc.init(r)
    var i = 0
    for(x <- added) {
      calc.add(r,i,1)
      i += 1
    }

    i = 0
    for(x <- removed) {
      calc.remove(r,i,2)
      i += 1
    }
    calc.setValue(0,0)
    calc.result.get(0,0)
  }

  def getDoubleCellwiseResult[T <: FocalOp[Raster]](createOp:(Raster,Neighborhood)=>T, n:Neighborhood,
                                              added:Seq[Int],removed:Seq[Int]):Double = {
    val r = MockCursor.fromAddRemove(added,removed).raster
    val op = createOp(r,n)
    val calc = op.getCalculation(r,n).asInstanceOf[CellwiseCalculation[Raster] with Initialization]
    calc.init(r)
    var i = 0
    for(x <- added) {
      calc.add(r,i,1)
      i += 1
    }

    i = 0
    for(x <- removed) {
      calc.remove(r,i,2)
      i += 1
    }
    calc.setValue(0,0)
    calc.result.getDouble(0,0)
  }

  // Default Raster for testing focal operations, constructed in a way
  // to give varying cases.
  def defaultRaster = {
    val N = NODATA
    createRaster(Array[Int]( 1, 3, 2,  4, 5, 2,  8, 4, 6, 9, 
                             1, 3, 2,  4, 5, 2, -2, 4, 6, 9, 
                             1, 3,-9,  4,-5, 2, 10,-4, 6, 9, 
                             1, 3, 2,-33, 5, 2, 88, 4, 6, 9, 
                             N, 3, 2,  4, 5, 2,  5, 4, 6, 9, 
                             1, 3, 2,  4, 0, 8, 33, 4, 6, 9, 
                             1, 3, 2, 10, 5, 2, 10, N, 6, 9, 
                             1, 3, 2,  4, 5, 1,-23,-4, 6, 9, 
                             7, 3, 2,  2, 2, 2, 70, 4, N, 9, 
                             1, 3, 2,-24, 5, 0,  2, 4, 6, 9))
  }

  val defaultTestSets:Seq[Seq[Int]] = Seq(Seq(NODATA,NODATA,1,-23,23),
                                   Seq(NODATA,NODATA,NODATA),
                                   Seq(1,2,3,4,5,6,7,8,9),
                                   Seq(-1,-2,-3,-4,-5,-6,-7,-8,-9),
                                   Seq(-1000,-100,-10,0,10,100,100))

}
