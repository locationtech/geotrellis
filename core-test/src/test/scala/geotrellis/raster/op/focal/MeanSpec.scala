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

package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.process._
import geotrellis.source._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

import scala.math._

import scalaxy.loops._

class MeanSpec extends FunSpec with FocalOpSpec
                               with TestServer
                               with ShouldMatchers {

  val getCursorMeanResult = (getDoubleCursorResult _).curried((r,n) => Mean(r,n))(Circle(1))
  val getCellwiseMeanResult = Function.uncurried((getDoubleCellwiseResult _).curried((r,n) => Mean(r,n))(Square(1)))

  describe("Mean") {
    it("should handle all NODATA") {
      isNoData(getCursorMeanResult(MockCursor.fromAll(NODATA,NODATA,NODATA,NODATA))) should be (true)
    }

    it("should match histogram mean default set in cursor calculation") {      
      for(s <- defaultTestSets) {
        val sf = s.filter { x => isData(x) }
        val expected = sf.sum / sf.length.toDouble
        if(isNoData(expected)) {
          isNoData(getCursorMeanResult(MockCursor.fromAddRemove(s,Seq[Int]()))) should equal (true)
        } else {
          getCursorMeanResult(MockCursor.fromAddRemove(s,Seq[Int]())) should equal (expected)
        }
      }
    }

    it("should match histogram mean default set in cellwise calculation") {      
      for(s <- defaultTestSets) {
        val sf = s.filter { x => isData(x) }
        val expected = sf.sum / sf.length.toDouble
        if(isNoData(expected)) {
          isNoData(getCellwiseMeanResult(s,Seq[Int]())) should equal (true)
        } else {
          getCellwiseMeanResult(s,Seq[Int]()) should equal (expected)
        }
      }
    }

    it("should hold state correctly with cursor calculation") {
      testDoubleCursorSequence((r,n) => Mean(r,n), Circle(1),
             Seq( SeqTestSetup(Seq(1,2,3,4,5), Seq[Int](), 3.0),
                  SeqTestSetup(Seq(10,10)    , Seq(2,3,5), 6.25)) )
    }

    it("should hold state correctly with cellwise calculation") {
      testDoubleCellwiseSequence((r,n)=>Mean(r,n), Square(1),
             Seq( SeqTestSetup(Seq(1,2,3,4,5), Seq[Int](), 3.0),
                  SeqTestSetup(Seq(10,10)    , Seq(2,3,5), 6.25)) )
    }

    it("should square min for raster source") {
      val rs1 = createRasterSource(
        Array( nd,7,1,      1,3,5,      9,8,2,
                9,1,1,      2,2,2,      4,3,5,

                3,8,1,      3,3,3,      1,2,2,
                2,4,7,     1,nd,1,      8,4,3
        ),
        3,2,3,2
      )

      run(rs1.focalMean(Square(1))) match {
        case Complete(result,success) =>
//          println(success)
          assertEqual(result,
            Array(5.666,  3.8,2.166,    1.666,   2.5, 4.166,    5.166, 5.166,   4.5,
                    5.6,3.875,2.777,    1.888, 2.666, 3.555,    4.111,   4.0, 3.666,

                    4.5,  4.0,3.111,      2.5, 2.125,   3.0,    3.111, 3.555, 3.166,
                   4.25,4.166,  4.0,      3.0,   2.2,   3.2,    3.166, 3.333,  2.75), 
            threshold = 0.001)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)

      }
    }


    it("should circle min for raster source") {
      val rs1 = createRasterSource(
            Array(5.666,  3.8,2.166,    1.666,   2.5, 4.166,    5.166, 5.166,   4.5,
                    5.6,3.875,2.777,    1.888, 2.666, 3.555,    4.111,   4.0, 3.666,

                    4.5,  4.0,3.111,      2.5, 2.125,   3.0,    3.111, 3.555, 3.166,
                   4.25,4.166,  4.0,      3.0,   2.2,   3.2,    3.166, 3.333,  2.75
            ),
        3,2,3,2
      )

      run(rs1.focalMean(Circle(1))) match {
        case Complete(result,success) =>
          //println(success)
          assertEqual(result,
            Array(5.022,3.876,2.602,    2.054, 2.749, 3.846,    4.652, 4.708, 4.444,
                  4.910, 4.01,2.763,    2.299, 2.546, 3.499,    3.988, 4.099, 3.833,

                  4.587, 3.93,3.277,    2.524, 2.498, 2.998,    3.388, 3.433, 3.284,
                  4.305,4.104,3.569,    2.925, 2.631, 2.891,    3.202, 3.201, 3.083
            ), threshold = 0.001)
        case Error(msg,failure) =>
          // println(msg)
          // println(failure)
          assert(false)
      }
    }

    it("should run Mean square 3 on tiled raster in catalog") {
      val name = "SBN_inc_percap"

      val source = RasterSource(name)
      val r = source.get

      val expected = source.focalMean(Square(3)).get

      val tileLayout = TileLayout.fromTileDimensions(r.rasterExtent,256,256)
      val rs = RasterSource(TileRaster.wrap(r,tileLayout,cropped = false))

      rs.focalMean(Square(3)).run match {
        case Complete(value,hist) =>
          for(col <- 0 until expected.cols optimized) {
            for(row <- 0 until expected.rows optimized) {
              withClue (s"Value different at $col,$row: ") {
                val v1 = expected.getDouble(col,row)
                val v2 = value.getDouble(col,row)
                if(isNoData(v1)) isNoData(v2) should be (true)
                else if(isNoData(v2)) isNoData(v1) should be (true)
                else v1 should be (v2)
              }
            }
          }
        case Error(message,trace) =>
          println(message)
          assert(false)
      }
    }
  }
}
