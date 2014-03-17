/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.zonal.summary

import geotrellis._
import geotrellis.feature._
import geotrellis.process._
import geotrellis.testkit._

import org.scalatest.FunSpec
import org.scalatest.matchers._

class HistogramSpec extends ZonalSummarySpec {
  describe("zonalHistogram") {
    it("computes Histogram") {
      val rData = createRasterSource(Array.fill(40*40)(1),4,4,10,10)
      val zone = Extent(10,-10,30,10).asFeature()

      val histOp = rData.zonalHistogram(zone)
      run(histOp) match {
        case Complete(result,success) =>
          //           println(success)
          result.getItemCount(1) should equal (40)
          result.getItemCount(2) should equal (0)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("computes Histogram for raster source and 5 edge polygon") {
      val h = statistics.FastMapHistogram()

      for(z <- containedCells.map { case (col,row) => tiledR.get(col,row) }) {
        if (isData(z)) { h.countItem(z, 1) }
      }

      run(tiledRS.zonalHistogram(poly)) match {
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
}
