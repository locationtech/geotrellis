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

package geotrellis.raster.op.local

import geotrellis.raster._
import geotrellis.feature.Extent
import geotrellis.engine._

import org.scalatest._
import geotrellis.testkit._

class LocalMapSpec extends FunSpec 
                      with Matchers 
                      with TestEngine 
                      with TileBuilders {
  describe("RasterSource methods") {
    it("should map an integer function over an integer raster source") {
      val rs = createRasterSource(
        Array(nd,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,nd),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMap(z => if(isNoData(z)) 0 else z + 1)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(r.get(col,row) == nd) { result.get(col,row) should be (0) }
              else { result.get(col,row) should be (2) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should map an integer function over a double raster source") {
      val rs = createRasterSource(
        Array(NaN,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,NaN),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMap(z => if(isNoData(z)) 0 else z + 1)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(isNoData(r.getDouble(col,row))) { result.getDouble(col,row) should be (0.0) }
              else { result.getDouble(col,row) should be (2.0) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should map a double function over an integer raster source") {
      val rs = createRasterSource(
        Array(nd,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,nd),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMapDouble(z => if(isNoData(z)) 0.0 else z + 1.0)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(r.get(col,row) == nd) { result.get(col,row) should be (0) }
              else { result.get(col,row) should be (2) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should map a double function over a double raster source") {
      val rs = createRasterSource(
        Array(NaN,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,NaN),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMapDouble(z => if(isNoData(z)) 0.0 else z + 0.3)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(isNoData(r.getDouble(col,row))) { result.getDouble(col,row) should be (0.0) }
              else { result.getDouble(col,row) should be (1.8) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should mapIfSet an integer function over an integer raster source") {
      val rs = createRasterSource(
        Array(nd,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,nd),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMapIfSet(z => z + 1)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(r.get(col,row) == nd) { result.get(col,row) should be (nd) }
              else { result.get(col,row) should be (2) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should mapIfSet an integer function over a double raster source") {
      val rs = createRasterSource(
        Array(NaN,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,NaN),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMapIfSet(z => z + 1)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(isNoData(r.getDouble(col,row))) { isNoData(result.getDouble(col,row)) should be (true) }
              else { result.getDouble(col,row) should be (2.0) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should mapIfSet a double function over an integer raster source") {
      val rs = createRasterSource(
        Array(nd,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,nd),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMapIfSetDouble{z:Double => if(isNoData(z)) 0.0 else z + 1.0}) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(r.get(col,row) == nd) { result.get(col,row) should be (nd) }
              else { result.get(col,row) should be (2) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should mapIfSet a double function over a double raster source") {
      val rs = createRasterSource(
        Array(NaN,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,NaN),
        3,2,3,2)

      val r = get(rs)
      run(rs.localMapIfSetDouble{z:Double => if(isNoData(z)) 0.0 else z + 0.3}) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(isNoData(r.getDouble(col,row))) { isNoData(result.getDouble(col,row)) should be (true) }
              else { result.getDouble(col,row) should be (1.8) }
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

