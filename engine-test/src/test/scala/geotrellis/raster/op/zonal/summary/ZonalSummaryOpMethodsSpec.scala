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

package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.raster.stats._
import geotrellis.vector._
import geotrellis.engine._
import geotrellis.testkit._

import org.scalatest._
class ZonalSummaryOpMethodsSpec extends FunSpec
                                   with Matchers
                                   with TestEngine
                                   with TileBuilders {  
  val tiledRS = 
    createRasterSource(
      Array(  1, 2, 3,   4, 5, 6,   7, 8, 9,
              1, 2, 3,   4, 5, 6,   7, 8, 9,

             10,11,12,  13,nd,14,  nd,15,16,
             11,12,13,  nd,15,nd,  17,18,19
      ),
      3,2,3,2)

  val tiledRSDouble = 
    createRasterSource(
      Array(  0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
              0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

             0.11,0.12,0.13,   0.14,NaN,0.15,   NaN, 0.16,0.17,
             0.11,0.12,0.13,    NaN,0.15,NaN,  0.17, 0.18,0.19
      ),
      3,2,3,2)

  val rasterExtent = tiledRS.rasterExtent.get
  val tiledR = get(tiledRS)
  val tiledRDouble = get(tiledRSDouble)

  val poly = {
    val re = rasterExtent
    val polyPoints = Seq(
      re.gridToMap(2,1), re.gridToMap(4,0),re.gridToMap(7,2),
      re.gridToMap(5,3), re.gridToMap(2,2),re.gridToMap(2,1)
    )
    Polygon(Line(polyPoints.map{Point.apply}))
  }

//      Polygon vertices are 0's (not contianed cells)
//      X's are contained cells
// 
//       *  *  *    *  *  *    *  *  *
//       *  *  0    X  X  X    *  *  *
// 
//       *  *  0    X  X  X    X  0  *
//       *  *  *    *  *  *    *  *  *  
// 

  val containedCells = Seq(
    (3,1),(4,1),(5,1),
    (3,2),(4,2),(5,2),(6,2)
  )

  describe("zonalHistogram") {
    it("computes Histogram for raster source and 5 edge polygon") {
      val h = FastMapHistogram()

      for(z <- containedCells.map { case (col,row) => tiledR.get(col,row) }) {
        if (isData(z)) { h.countItem(z, 1) }
      }

      tiledRS.zonalHistogram(poly).run match {
        case Complete(result,success) =>
          //           println(success)
          for(row <- 0 until tiledR.rows) {
            for(col <- 0 until tiledR.cols) {
              val v = tiledR.get(col,row)
              if(isData(v)) {
                result.getItemCount(v) should be (h.getItemCount(v))
              }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalMax") {
    it("computes max on raster source and 5 edge polygon") {
      val max = 
        containedCells
          .map { case (col,row) => tiledR.get(col,row) }
          .foldLeft(Int.MinValue) { (a,b) => if(isNoData(b)) a else math.max(a, b) }

      run(tiledRS.zonalMax(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (max)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes max on double raster source and 5 edge polygon") {
      val max = 
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(Double.MinValue) { (a,b) => if(isNoData(b)) a else math.max(a, b) }

      run(tiledRSDouble.zonalMaxDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (max)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalMean") {
    it("computes sum on raster source and 5 edge polygon") {
      val vals =
        containedCells
          .map { case (col,row) => tiledR.getDouble(col,row) }
      val sum =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + b }
      val count =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + 1.0 }

      run(tiledRS.zonalMean(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum / count)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes mean on double raster source and 5 edge polygon") {
      val vals =
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
      val sum =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + b }
      val count =
        vals
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + 1.0 }

      run(tiledRSDouble.zonalMean(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum / count.toDouble)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalMin") {
    it("computes min on raster source and 5 edge polygon") {
      val min =
        containedCells
          .map { case (col,row) => tiledR.get(col,row) }
          .foldLeft(Int.MaxValue) { (a,b) => if(isNoData(b)) a else math.min(a, b) }

      run(tiledRS.zonalMin(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (min)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
    it("computes min on double raster source and 5 edge polygon") {
      val min =
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(Double.MaxValue) { (a,b) => if(isNoData(b)) a else math.min(a, b) }

      run(tiledRSDouble.zonalMinDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (min)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("zonalSum") {
    it("computes sum on raster source and 5 edge polygon") {
      val sum = 
        containedCells
          .map { case (col,row) => tiledR.get(col,row) }
          .foldLeft(0) { (a,b) => if(isNoData(b)) a else a + b }

      run(tiledRS.zonalSum(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes Double sum on raster source and 5 edge polygon") {
      val sum = 
        containedCells
          .map { case (col,row) => tiledRDouble.getDouble(col,row) }
          .foldLeft(0.0) { (a,b) => if(isNoData(b)) a else a + b }

      run(tiledRSDouble.zonalSumDouble(poly)) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (sum)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("using cached results") {
    it("computes mean on double raster source with cached results") {
      val rs =
        createRasterSource(
          Array(  0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

            0.11,0.12,0.13,   0.14,NaN,0.15,  NaN, 0.16,0.17,
            0.11,0.12,0.13,   NaN,0.15,NaN,   0.17, 0.18,0.19,

            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,

            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9,
            0.1, 0.2, 0.3,   0.4, 0.5, 0.6,   0.7, 0.8, 0.9
          ),
          3,4,3,2)

      val tr = rs.get
      val re = rs.rasterExtent.get

      val p = {
        val polyPoints = Seq(
          Point(re.gridToMap(1,1)), Point(re.gridToMap(2,0)), Point(re.gridToMap(4,0)), Point(re.gridToMap(7,2)),
          Point(re.gridToMap(6,6)), Point(re.gridToMap(1,6)), Point(re.gridToMap(0,3)), Point(re.gridToMap(1,1))
        )
        Polygon(Line(polyPoints))
      }

      var usedCached = false

      val cached =
        rs.map(tile => { usedCached = true ; MeanResult.fromFullTileDouble(tile)})
          .cached

      val withCached = rs.zonalMean(p,cached)
      val withoutCached = rs.zonalMean(p)

      usedCached should be (true)
      withCached.get should be (withoutCached.get)
    }
  }
}
