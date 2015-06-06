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
import geotrellis.engine._
import geotrellis.engine.io._
import org.scalatest._

import spire.syntax.cfor._

object TestEngine {
  private var _init = false

  lazy val init =
    if(!_init) {
      GeoTrellis.init(GeoTrellisConfig(), "test-server")
      _init = true
    }
}

trait TestEngine extends Suite with BeforeAndAfter with Matchers {
  implicit lazy val engine: geotrellis.engine.Engine = {
    val config = GeoTrellisConfig()
    val name = s"engine-${getClass.getName}"

    val catalog = config.catalogPath match {
      case Some(path) =>
        val file = new java.io.File(path)
        if(!file.exists()) {
          sys.error(s"Catalog path ${file.getAbsolutePath} does not exist. Please modify your settings.")
        }
        Catalog.fromPath(file.getAbsolutePath)
      case None => Catalog.empty(s"${name}-catalog")
    }

    Engine(name, catalog)
  }

  def run[T](op: Op[T]): OperationResult[T] = { engine.run(op) }
  def run[T](src: OpSource[T]): OperationResult[T] = { engine.run(src) }
  def get[T](op: Op[T]): T = { engine.get(op) }
  def get[T](src: OpSource[T]): T = { engine.get(src) }

  def getRaster(name: String): Op[Tile] = getRaster("test:fs", name)
  def getRaster(ds: String, name: String): Op[Tile] = LoadRaster(ds, name)

  def assertEqual(r: Op[Tile], arr: Array[Int]): Unit =
    assertEqual(get(r), arr)

  def assertEqual(r: Tile, arr: Array[Int]): Unit = {
    withClue(s"Sizes do not match.") {
      (r.cols * r.rows) should be (arr.length)
    }

    r.foreach { (col, row, z) =>
      withClue(s"Value at ($col, $row) are not the same") {
        z should be (arr(row * r.cols + col))
      }
    }
  }

  def assertEqual(r: Op[Tile], arr: Array[Double]): Unit =
    assertEqual(r, arr, 0.0000000001)

  def assertEqual(r: Op[Tile], arr: Array[Double], threshold: Double): Unit = {
    val raster = get(r)
    val cols = raster.cols
    val rows = raster.rows

    raster.foreachDouble { (col, row, v1) =>
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

  def assertEqual(r: Op[Tile], r2: Op[Tile]): Unit = assertEqual(r, r2, 0.0000000001)

  def assertEqual(rOp1: Op[Tile], rOp2: Op[Tile], threshold: Double): Unit = {
    val r1 = get(rOp1)
    val r2 = get(rOp2)

    withClue("Columns are not equal") { r1.cols should be (r2.cols) }
    withClue("Rows are not equal") { r1.rows should be (r2.rows) }
    withClue("cellTypes are not equal") { r1.cellType should be (r2.cellType) }

    val isFloatingPoint = r1.cellType.isFloatingPoint

    if(isFloatingPoint) {
      r1.foreachDouble { (col, row, v1) =>
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
      }
    } else {
      r1.foreach { (col, row, v1) =>
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
