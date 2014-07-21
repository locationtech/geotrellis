///*
// * Copyright (c) 2014 Azavea.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package geotrellis.raster.op.focal
//
//import geotrellis.raster._
//import geotrellis.feature.Extent
//import geotrellis.raster.op.local._
//import geotrellis.engine._
//
//import geotrellis.testkit._
//
//import org.scalatest._
//
//import scala.math._
//
//case class SeqTestSetup[@specialized(Int, Double)D](adds: Seq[Int], removes: Seq[Int], result: D)
//
//case class CursorSetup(r: Tile, calc: CursorCalculation[Tile], cursor: Cursor) {
//  def result(x: Int, y: Int) = {
//    cursor.centerOn(x, y)
//    calc.calc(r, cursor)
//    calc.result.get(x, y)
//  }
//}
//
//object MockCursorHelper {
//  def raster = IntArrayTile.empty(3, 3)
//  def analysisArea = GridBounds(raster)
//}
//
//object MockCursor {
//  def fromAll(s: Int*) = {
//    new MockCursor(s, Seq[Int](), Seq[Int]())
//  }
//
//  def fromAddRemove(a: Seq[Int], r: Seq[Int]) = {
//    new MockCursor(Seq[Int](), a, r)
//  }
//
//  def fromAddRemoveAll(all: Seq[Int], a: Seq[Int], r: Seq[Int]) = {
//    new MockCursor(all, a, r)
//  }
//
//
//}
//
//case class MockCursor(all: Seq[Int], added: Seq[Int], removed: Seq[Int]) extends Cursor(MockCursorHelper.raster, MockCursorHelper.analysisArea, 1) {
//  centerOn(0, 0)
//
//  override val allCells = new CellSet {
//    def foreach(f: (Int, Int)=>Unit) = {
//      var i = 0
//      for(x <- all) {
//        f(i, 0)
//        i += 1
//      }
//    }
//  }
//
//  override val addedCells = new CellSet {
//    def foreach(f: (Int, Int)=>Unit) = {
//      var i = 0
//      for(x <- added) {
//        f(i, 1)
//        i += 1
//      }
//    }
//
//  }
//
//  override val removedCells = new CellSet {
//    def foreach(f: (Int, Int)=>Unit) = {
//      var i = 0
//      for(x <- removed) {
//        f(i, 2)
//        i += 1
//      }
//    }
//  }
//
//  def raster = {
//    val cols = max(all.length, max(added.length, removed.length))
//    val data = Array.ofDim[Int](cols, 3)
//    var i = 0
//    val c = all ++ { for(x <- 0 until (cols - all.length)) yield 0 }
//    val a = added ++ { for(x <- 0 until (cols - added.length)) yield 0 }
//    val r = removed ++ { for(x <- 0 until (cols - removed.length)) yield 0 }
//    val d = (c ++ a ++ r).toArray
//    ArrayTile(d, cols, 3)
//  }
//}
//
//
//trait FocalOpSpec extends TileBuilders with Matchers {
//  def getSetup[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, r: Tile, n: Neighborhood) = {
//    val op = createOp(r, n)
//    val calc = op.getCalculation(r, n).asInstanceOf[CursorCalculation[Tile] with Initialization]
//    calc.init(r)
//    val analysisArea = GridBounds(r)
//    CursorSetup(r, calc, Cursor(r, n, analysisArea))
//  }
//
//  def getCursorResult[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood, cursor: MockCursor): Int = {
//    val r = cursor.raster
//    val op = createOp(r, n)
//    val calc = op.getCalculation(r, n).asInstanceOf[CursorCalculation[Tile] with Initialization]
//    calc.init(r)
//    calc.calc(r, cursor)
//    calc.result.get(0, 0)
//  }
//
//  def getDoubleCursorResult[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood, cursor: MockCursor): Double = {
//    val r = cursor.raster
//    val op = createOp(r, n)
//    val calc = op.getCalculation(r, n).asInstanceOf[CursorCalculation[Tile] with Initialization]
//    calc.init(r)
//    calc.calc(r, cursor)
//    calc.result.getDouble(0, 0)
//  }
//
//  def testCursorSequence[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood,
//                                               setups: Seq[SeqTestSetup[Int]]) = {
//    val op = createOp(MockCursorHelper.raster, n)
//    val calc = op.getCalculation(MockCursorHelper.raster, n).asInstanceOf[CursorCalculation[Tile] with Initialization]
//
//    var init = true
//    for(setup <- setups) {
//      val mockCursor = MockCursor.fromAddRemove(setup.adds, setup.removes)
//      if(init) { calc.init(mockCursor.raster) ; init = false }
//      calc.calc(mockCursor.raster, mockCursor)
//      calc.result.get(0, 0) should equal(setup.result)
//    }
//  }
//
//  def testCellwiseSequence[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood,
//                                               setups: Seq[SeqTestSetup[Int]]) = {
//    val op = createOp(MockCursorHelper.raster, n)
//    val calc = op.getCalculation(MockCursorHelper.raster, n).asInstanceOf[CellwiseCalculation[Tile] with Initialization]
//
//    var init = true
//    for(setup <- setups) {
//      val r = MockCursor.fromAddRemove(setup.adds, setup.removes).raster
//      if(init) { calc.init(r) ; init = false }
//      var i = 0
//      for(x <- setup.adds) {
//        calc.add(r, i, 1)
//        i += 1
//      }
//      i = 0
//      for(x <- setup.removes) {
//        calc.remove(r, i, 2)
//        i += 1
//      }
//      calc.setValue(0, 0)
//      calc.result.get(0, 0) should equal (setup.result)
//    }
//  }
//
//  def testDoubleCursorSequence[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood,
//                                               setups: Seq[SeqTestSetup[Double]]) = {
//    val op = createOp(MockCursorHelper.raster, n)
//    val calc = op.getCalculation(MockCursorHelper.raster, n).asInstanceOf[CursorCalculation[Tile] with Initialization]
//
//    var init = true
//    for(setup <- setups) {
//      val mockCursor = MockCursor.fromAddRemove(setup.adds, setup.removes)
//      if(init) { calc.init(mockCursor.raster) ; init = false }
//      calc.calc(mockCursor.raster, mockCursor)
//      calc.result.getDouble(0, 0) should equal(setup.result)
//    }
//  }
//
//  def testDoubleCellwiseSequence[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood,
//                                               setups: Seq[SeqTestSetup[Double]]) = {
//    val op = createOp(MockCursorHelper.raster, n)
//    val calc = op.getCalculation(MockCursorHelper.raster, n).asInstanceOf[CellwiseCalculation[Tile] with Initialization]
//
//    var init = true
//    for(setup <- setups) {
//      val r = MockCursor.fromAddRemove(setup.adds, setup.removes).raster
//      if(init) { calc.init(r) ; init = false }
//      var i = 0
//      for(x <- setup.adds) {
//        calc.add(r, i, 1)
//        i += 1
//      }
//      i = 0
//      for(x <- setup.removes) {
//        calc.remove(r, i, 2)
//        i += 1
//      }
//      calc.setValue(0, 0)
//      calc.result.getDouble(0, 0) should equal (setup.result)
//    }
//  }
//
//
//  def getCellwiseResult[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood,
//                                              added: Seq[Int], removed: Seq[Int]) = {
//    val r = MockCursor.fromAddRemove(added, removed).raster
//    val op = createOp(r, n)
//    val calc = op.getCalculation(r, n).asInstanceOf[CellwiseCalculation[Tile] with Initialization]
//    calc.init(r)
//    var i = 0
//    for(x <- added) {
//      calc.add(r, i, 1)
//      i += 1
//    }
//
//    i = 0
//    for(x <- removed) {
//      calc.remove(r, i, 2)
//      i += 1
//    }
//    calc.setValue(0, 0)
//    calc.result.get(0, 0)
//  }
//
//  def getDoubleCellwiseResult[T <: FocalOperation0[Tile]](createOp: (Tile, Neighborhood)=>T, n: Neighborhood,
//                                              added: Seq[Int], removed: Seq[Int]): Double = {
//    val r = MockCursor.fromAddRemove(added, removed).raster
//    val op = createOp(r, n)
//    val calc = op.getCalculation(r, n).asInstanceOf[CellwiseCalculation[Tile] with Initialization]
//    calc.init(r)
//    var i = 0
//    for(x <- added) {
//      calc.add(r, i, 1)
//      i += 1
//    }
//
//    i = 0
//    for(x <- removed) {
//      calc.remove(r, i, 2)
//      i += 1
//    }
//    calc.setValue(0, 0)
//    calc.result.getDouble(0, 0)
//  }
//
//  // Default Tile for testing focal operations, constructed in a way
//  // to give varying cases.
//  def defaultRaster = {
//    val N = NODATA
//    createTile(Array[Int]( 1, 3, 2,  4, 5, 2,  8, 4, 6, 9,
//                           1, 3, 2,  4, 5, 2, -2, 4, 6, 9,
//                           1, 3, -9, 4,-5, 2, 10,-4, 6, 9,
//                           1, 3, 2,-33, 5, 2, 88, 4, 6, 9,
//                           N, 3, 2,  4, 5, 2,  5, 4, 6, 9,
//                           1, 3, 2,  4, 0, 8, 33, 4, 6, 9,
//                           1, 3, 2, 10, 5, 2, 10, N, 6, 9,
//                           1, 3, 2,  4, 5, 1,-23,-4, 6, 9,
//                           7, 3, 2,  2, 2, 2, 70, 4, N, 9,
//                           1, 3, 2,-24, 5, 0,  2, 4, 6, 9))
//  }
//
//  val defaultTestSets: Seq[Seq[Int]] = Seq(Seq(NODATA, NODATA, 1, -23, 23),
//                                   Seq(NODATA, NODATA, NODATA),
//                                   Seq(1, 2, 3, 4, 5, 6, 7, 8, 9),
//                                   Seq(-1, -2, -3, -4, -5, -6, -7, -8, -9),
//                                   Seq(-1000, -100, -10, 0, 10, 100, 100))
//
//}
