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

package geotrellis.raster

import geotrellis.engine._
import geotrellis.feature.Extent
import geotrellis.testkit._

import geotrellis.raster.stats._
import geotrellis.raster.op._

import org.scalatest._

class RasterSourceSpec extends FunSpec 
                          with Matchers 
                          with TestEngine 
                          with TileBuilders {
  def getRasterSource = 
    RasterSource("mtsthelens_tiled_cached")

  def getSmallRasterSource =
    RasterSource("quad_tiled")

  describe("convert") {
    it("converts an integer RasterSource to a double RasterSource") {
      val rs = createRasterSource(
        Array( 1,10,100,1000,2,2,2,2,2,
               2,20,200,2000,2,2,2,2,2,
               3,30,300,3000,2,2,2,2,2,
               4,40,400,4000,2,2,2,2,2),
        9,4)
      rs.convert(TypeDouble).get.cellType should be (TypeDouble)
    }
  }

  describe("RasterSource") {
    it("should load a tiled raster with a target extent") {
      val rasterExtent @ RasterExtent(Extent(xmin,_,_,ymax),cw,ch,_,_) =
        RasterSource("mtsthelens_tiled")
          .rasterExtent
          .get

      val newRe = 
        RasterExtent(
          Extent(xmin,ymax-(ch*256),xmin+(cw*256),ymax),
          cw,ch,256,256)

      val uncropped = RasterSource("mtsthelens_tiled").get
      val cropped = RasterSource("mtsthelens_tiled",newRe).get

      var count = 0
      for(row <- 0 until 256) {
        for(col <- 0 until 256) {
          withClue(s"Failed at ($col,$row)") {
            uncropped.get(col,row) should be (cropped.get(col,row))
          }
        }
      }
    }

    it("should match tiled and non-tiled rasters") {
      val layer = get(io.LoadRasterLayer("SBN_inc_percap_tiled"))
      val tiled = layer.getRaster
      val nonTiledRe = RasterSource("SBN_inc_percap").rasterExtent.get
      val nonTiled = RasterSource("SBN_inc_percap").get

      // println(s"${layer.info.rasterExtent} ${tiled.cols}, ${tiled.rows}")
      // println(s"$nonTiledRe ${nonTiled.cols}, ${nonTiled.rows}")

      for(row <- 0 until nonTiled.rows) {
        for(col <- 0 until nonTiled.cols) {
          withClue(s"Failed at ($col,$row)") {
            nonTiled.get(col,row) should be (tiled.get(col,row))
          }
        }
      }
    }

    it("should converge a tiled raster") {
      val s = 
        RasterSource("mtsthelens_tiled_cached")
          .renderPng

      get(s)

    }

    it("should return a RasterSource when possible") { 
      val d1 = getRasterSource

      val d2:RasterSource = d1.localAdd(3)
      val d3:RasterSource  = d2 map(local.Add(_, 3))
      val d4:RasterSource = d3 map(r => r.map(z => z + 3))
      val d5:DataSource[Int,Seq[Int]] = d3 map(r => r.findMinMax._2)
      
      val result1 = get(d1)
      val result2 = get(d2)
      val result3 = get(d3)
      val result4 = get(d4)
      val result5 = get(d5)

      result1.get(100,100) should be (3233)
      result2.get(100,100) should be (3236)
      result3.get(100,100) should be (3239)
      result4.get(100,100) should be (3242)
      result5.head should be (6026)
    }

    it("should return a RasterSource when calling .distribute") {
      val d1:RasterSource = (getRasterSource + 3).distribute
    }

    it("should handle a histogram result") {
      val d = getRasterSource

      val hist = d.tileHistograms
      val hist2:DataSource[Histogram,Histogram] = d.map( (h:Raster) => FastMapHistogram() )
      case class MinFromHistogram(h:Op[Histogram]) extends Op1(h)({
        (h) => Result(h.getMinValue)
      })

      case class FindMin(ints:Op[Seq[Int]]) extends Op1(ints)({
        (ints) => Result(ints.reduce(math.min(_,_)))
      })

      val ints:DataSource[Int,Seq[Int]] = hist.mapOp(MinFromHistogram(_))
     
      val seqIntVS:ValueSource[Seq[Int]] = ints.converge

      val intVS:ValueSource[Int] = seqIntVS.map( seqInt => seqInt.reduce(math.min(_,_)))
      val intVS2 = ints.reduce(math.min(_,_))

      val histogramResult = get(hist)
      val intsResult = get(ints)

      val intResult = get(intVS)
      val directIntResult = get(intVS2)

      histogramResult.getMinValue should be (2231)
      histogramResult.getMaxValue should be (8367)
      intsResult.length should be (12)
      intResult should be (directIntResult)
      
    }

    it("should handle combine") {
      val d = getRasterSource
      val d2 = getRasterSource

      val combineDS:RasterSource = d.localCombine(d2)(_+_)
      val initial = get(d)
      val result = get(combineDS)

      result.get(3,3) should be (initial.get(3,3) * 2)
    }
  }

  describe("warp") {
    it("should warp with crop only on single tile") {
      // val rs = createRasterSource(
      //   Array( 1,10,100,1000,2,2,2,2,2,
      //          2,20,200,2000,2,2,2,2,2,
      //          3,30,300,3000,2,2,2,2,2,
      //          4,40,400,4000,2,2,2,2,2),
      //   1,1,9,4)

      val rs = createRasterSource(
        Array( 1,10,100,1000,2,2,2,2,2,
               2,20,200,2000,2,2,2,2,2,
               3,30,300,3000,2,2,2,2,2,
               4,40,400,4000,2,2,2,2,2),
        9,4)


      val RasterExtent(Extent(xmin,_,_,ymax),cw,ch,cols,rows) = rs.rasterExtent.get
      val newRe = RasterExtent(Extent(xmin,ymax - (ch*3),xmin + (cw*4), ymax),4,3)
      rs.warp(newRe).run match {
        case Complete(r,_) =>
          assertEqual(r,Array(1,10,100,1000,
                              2,20,200,2000,
                              3,30,300,3000))
        case Error(msg,trace) =>
          println(msg)
          println(trace)
          assert(false)
      }
    }

    it("should warp with crop only with tiles") {
      val rs = createRasterSource(
        Array( 1,10,100, 1000,2,2, 2,2,2,
               2,20,200, 2000,2,2, 2,2,2,

               3,30,300, 3000,2,2, 2,2,2,
               4,40,400, 4000,2,2, 2,2,2),
        3,2,3,2)

      val RasterExtent(Extent(xmin,_,_,ymax),cw,ch,cols,rows) = rs.rasterExtent.get
      val newRe = RasterExtent(Extent(xmin,ymax - (ch*3),xmin + (cw*4), ymax),4,3)
      rs.warp(newRe).run match {
        case Complete(r,_) =>
          assertEqual(r,Array(1,10,100,1000,
                              2,20,200,2000,
                              3,30,300,3000))
        case Error(msg,trace) =>
          println(msg)
          println(trace)
          assert(false)
      }
    }

    it("should crop to a target Extent") {
      val rs = createRasterSource(
                 Array(1, 10, 100, 1000, 2, 2, 2, 2, 2,
                       2, 20, 200, 2000, 2, 2, 2, 2, 2,
                       3, 30, 300, 3000, 2, 2, 2, 2, 2,
                       4, 40, 400, 4000, 2, 2, 2, 2, 2),
                 9, 4)

      val RasterExtent(Extent(xmin, ymin, _, _), cw, ch, _, _) = rs.rasterExtent.get
      val newExt = Extent(xmin, ymin, xmin + (cw * 4), ymin + (ch * 2))
      rs.warp(newExt).run match {
        case Complete(r,_) =>
          assertEqual(r, Array(3, 30, 300, 3000,
                               4, 40, 400, 4000))
        case Error(msg,trace) =>
          println(msg)
          println(trace)
          assert(false)
      }
    }

    it("should crop to target dimensions") {
      val rs = createRasterSource(
                 Array(2, 10, 2, 100, 2, 1000, 2, 10000,
                       2, 20, 2, 200, 2, 2000, 2, 20000,
                       2, 30, 2, 300, 2, 3000, 2, 30000,
                       2, 40, 2, 400, 2, 4000, 2, 40000),
                 8, 4)

      val RasterExtent(Extent(_, _, _, _), _, _, cols, rows) = rs.rasterExtent.get
      val newCols = cols - 4
      val newRows = rows - 2
      rs.warp(newCols, newRows).run match {
        case Complete(r,_) =>
          assertEqual(r, Array(10, 100, 1000, 10000,
                               30, 300, 3000, 30000))
        case Error(msg,trace) =>
          println(msg)
          println(trace)
          assert(false)
      }
    }
  }
}
