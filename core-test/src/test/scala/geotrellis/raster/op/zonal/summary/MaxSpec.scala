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

import geotrellis._
import geotrellis.feature._
import geotrellis.process._
import geotrellis.testkit._

class MaxSpec extends ZonalSummarySpec {
  describe("Max") {
    it("computes Maximum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).toPolygon

      val maxOp = rData.zonalMax(zone)
      run(maxOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

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

    it("computes Double Maximum") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).toPolygon

      val maxDOp = rData.zonalMaxDouble(zone)
      run(maxDOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1.0)
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
}
