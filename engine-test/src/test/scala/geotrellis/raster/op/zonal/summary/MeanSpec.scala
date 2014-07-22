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
import geotrellis.vector._
import geotrellis.engine._
import geotrellis.testkit._

class MeanSpec extends ZonalSummarySpec {
  describe("Mean") {
    it("computes Mean") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).toPolygon

      val meanOp = rData.zonalMean(zone)
      run(meanOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1.0)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

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

    it("computes Double Mean") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).toPolygon

      val meanDOp = rData.zonalMeanDouble(zone)
      run(meanDOp) match {
        case Complete(result,success) =>
          //           println(success)
          result should equal (1.0)
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

      run(tiledRSDouble.zonalMeanDouble(poly)) match {
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
}
