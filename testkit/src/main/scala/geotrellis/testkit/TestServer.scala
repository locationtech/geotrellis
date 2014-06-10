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

package geotrellis.testkit

import geotrellis.raster._
import geotrellis.raster.io._
import geotrellis.raster.op._
import geotrellis.engine._
import org.scalatest._

object TestServer {
  lazy val init = {
    GeoTrellis.init(GeoTrellisConfig("core-test/data/catalog.json"), "test-server")
  }
}

trait TestServer extends Suite with BeforeAndAfter with Matchers {
  TestServer.init

  def run[T](op: Op[T]): OperationResult[T] = GeoTrellis.run(op)
  def run[T](src: DataSource[_, T]): OperationResult[T] = GeoTrellis.run(src)
  def get[T](op: Op[T]): T = GeoTrellis.get(op)
  def get[T](src: DataSource[_, T]): T = GeoTrellis.get(src)

  def getRaster(name: String): Op[Tile] = getRaster("test:fs", name)
  def getRaster(ds: String, name: String): Op[Tile] = io.LoadRaster(ds, name)

  def assertEqual(r: Op[Tile], arr: Array[Int]): Unit =
    assertEqual(get(r), arr)

  def assertEqual(r: Tile, arr: Array[Int]): Unit = {
    (r.cols * r.rows) should be (arr.length)
    for(row <- 0 until r.rows) {
      for(col <- 0 until r.cols) {
        withClue(s"Value at ($col, $row) are not the same") {
          r.get(col, row) should be (arr(row * r.cols + col))
        }
      }
    }
  }

  def assertEqual(r: Op[Tile], arr: Array[Double]): Unit = 
    assertEqual(r, arr, 0.0000000001)

  def assertEqual(r: Op[Tile], arr: Array[Double], threshold: Double): Unit = {
    val raster = get(r)
    val cols = raster.cols
    val rows = raster.rows
    for(row <- 0 until rows) {
      for(col <- 0 until cols) {
        val v1 = raster.getDouble(col, row)
        val v2 = arr(row * cols + col)
        if(isNoData(v1)) {
          withClue(s"Value at ($col, $row) are not the same: v1 = NoData, v2 = $v2") {
            isNoData(v2) should be (true)
          }
        } else {
          if(isNoData(v2)) {
            withClue(s"Value at ($col, $row) are not the same: v1 = $v1, v2 = NoData") {
              isNoData(v1) should be (true)
            }
          } else {
            withClue(s"Value at ($col, $row) are not the same: ") {
              v1 should be (v2 +- threshold)
            }
          }
        }
      }
    }
  }


  def assertEqual(r: Op[Tile], r2: Op[Tile]): Unit = assertEqual(r, r2, 0.0000000001)

  def assertEqual(rOp1: Op[Tile], rOp2: Op[Tile], threshold: Double): Unit = {
    val r1 = get(rOp1)
    val r2 = get(rOp2)
    
    withClue("Columns are not equal") { r1.cols should be (r2.cols) }
    withClue("Rows are not equal") { r1.rows should be (r2.rows) }
    withClue("cellTypes are not equal") { r1.cellType should be (r2.cellType) }

    val isFloatingPoint = r1.cellType.isFloatingPoint
    for(col <- 0 until r1.cols) {
      for(row <- 0 until r1.rows) {
        if(isFloatingPoint) {
          val v1 = r1.getDouble(col, row)
          val v2 = r2.getDouble(col, row)

          if(isNoData(v1)) {
            withClue(s"Value at ($col, $row) are not the same: v1 = NoData, v2 = $v2") {
              isNoData(v2) should be (true)
            }
          }

          if(isNoData(v2)) {
            withClue(s"Value at ($col, $row) are not the same: v1 = $v1, v2 = NoData") {
              isNoData(v1) should be (true)
            }
          }

          if(math.abs(v1 - v2) >= threshold) {
            withClue(s"Failure at (${col}, ${row}) - V1: $v1  V2: $v2") {
              v1 should be (v2)
            }
          }
        } else {
          val v1 = r1.get(col, row)
          val v2 = r2.get(col, row)
          if(v1 != v2) {
            withClue(s"Failure at (${col}, ${row}) - V1: $v1  V2: $v2") {
              r1.get(col, row) should be (r2.get(col, row))
            }
          }
        }
      }
    }
  }
}
