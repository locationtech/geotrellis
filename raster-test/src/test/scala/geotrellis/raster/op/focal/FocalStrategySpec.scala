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

import geotrellis.raster._
import geotrellis.engine._
import geotrellis.feature.Extent

import org.scalatest._

import scala.collection.mutable.Set

class FocalStrategySpec extends FunSpec with Matchers {
  describe("CursorStrategy") {
    it("should execute the ZigZag traversal strategy correctly") {
      val rex = RasterExtent(Extent(0, 0, 5, 5), 1, 1, 5, 5)
      val d = 
        Array(
          1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
        )
      val r = ArrayTile(d, 5, 5)
      val cur = Cursor(r, Square(1))
      
      var lastY = 0
      var lastX = -1

      val calc = new CursorCalculation[Int](r, Square(1), None) {
        def result = 0
        def calc(r: Tile, cursor: Cursor) = {
          if(cursor.row != 0 || cursor.col != 0 ) { cursor.isReset should equal(false) }
          if(lastY != cursor.row) {
            cursor.row should be > lastY
            cursor.col should equal(lastX)
            lastY = cursor.row
          } else {
            if(cursor.row % 2 == 0) { (cursor.col - lastX) should equal (1) }
            else { (cursor.col - lastX) should equal (-1) }
          }
          lastX = cursor.col
        }
      }

      CursorStrategy.execute(r, cur, calc, TraversalStrategy.ZigZag, GridBounds(r))
    }

    it("should execute the ScanLine traversal strategy correctly") {
      val rex = RasterExtent(Extent(0, 0, 5, 5), 1, 1, 5, 5)
      val d = 
        Array(
          1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
        )
      val r = ArrayTile(d, 5, 5)
      val cur = Cursor(r, Square(1))
      
      var lastY = -1
      var lastX = 0

      val calc = new CursorCalculation[Int](r, Square(1), None) {
        def result = 0
        def calc(r: Tile, cursor: Cursor) = {
          if(lastY != cursor.row) {
            cursor.isReset should equal(true)
            cursor.row should be > lastY
            cursor.col should equal(0)
            lastY = cursor.row
          } else {
            cursor.col should be  > lastX
          }
          lastX = cursor.col
        }
      }

      CursorStrategy.execute(r, cur, calc, TraversalStrategy.ScanLine, GridBounds(r))
    }
  }
}
