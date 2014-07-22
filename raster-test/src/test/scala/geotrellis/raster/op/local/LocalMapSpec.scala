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

import org.scalatest._
import geotrellis.testkit._

class LocalMapSpec extends FunSpec 
                      with Matchers 
                      with TestEngine 
                      with TileBuilders {
  describe("LocalMap") {
    it ("performs integer function") {
      val r = positiveIntegerRaster
      val result = r.map { (x:Int) => x * 10 }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 10)
        }
      }
    }

    it ("performs integer function against TypeDouble raster") {
      val r = probabilityNoDataRaster
      val result = r.map { (x:Int) => if(isNoData(x)) NODATA else x * 10 }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { isNoData(result.getDouble(col,row)) should be (true) }
          else { result.getDouble(col,row) should be (r.getDouble(col,row).toInt * 10) }
        }
      }
    }

    it ("performs double function") {
      val r = probabilityRaster
      val result = r.mapDouble { (x:Double) => x * 10.0 }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 10)
        }
      }
    }

    it ("performs double function against TypeInt raster") {
      val r = positiveIntegerNoDataRaster
      val result = r.mapDouble { (x:Double) => x * 10.0 }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { result.get(col,row) should be (NODATA) }
          else { result.get(col,row) should be (r.get(col,row).toDouble * 10.0) }
        }
      }
    }

    it ("performs binary integer function") {
      val r = positiveIntegerRaster
      val result = r.combine(r) { (z1: Int,z2:Int) => z1+z2 }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 2)
        }
      }
    }

    it ("performs binary integer function against TypeDouble raster") {
      val r = probabilityNoDataRaster
      val result = r.combine(r) { (z1:Int,z2:Int) => if(isNoData(z1)) { NODATA} else { z1 + z2 } }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { isNoData(result.getDouble(col,row)) should be (true) }
          else { result.getDouble(col,row) should be (r.getDouble(col,row).toInt * 2) }
        }
      }
    }

    it ("performs binary double function") {
      val r = probabilityRaster
      val result = r.combineDouble(r) { (z1:Double,z2:Double) => z1+z2 }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 2)
        }
      }
    }

    it ("performs binary double function against TypeInt raster") {
      val r = positiveIntegerNoDataRaster
      val result = r.combineDouble(r) { (z1:Double,z2:Double) => if(isNoData(z1)) {NODATA} else {z1 + z2} }
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) { result.get(col,row) should be (NODATA) }
          else { result.get(col,row) should be (r.get(col,row) * 2) }
        }
      }
    }

    it ("works with int raster with NODATA values") {
      val rasterExtent = RasterExtent(Extent(0.0, 0.0, 100.0, 80.0), 20.0, 20.0, 5, 4)
      val nd = NODATA
      
      val data1 = Array(12, 12, 13, 14, 15,
                        44, 91, nd, 11, 95,
                        12, 13, 56, 66, 66,
                        44, 91, nd, 11, 95)

      val f2 = (a:Array[Int], cols:Int, rows:Int) => {
        ArrayTile(a, cols, rows)
      }

      val f = (a:Array[Int], cols:Int, rows:Int) => f2(a, cols, rows)
      
      val a = Array(1, 2, 3, 4, 5, 6, 7, 8, 9)
      val r = f(a, 3, 3)

      val r2 = r.map { z:Int => z + 1 }
      val d = r2.toArray
      d should be (a.map { _ + 1 })
    }
  }
}

