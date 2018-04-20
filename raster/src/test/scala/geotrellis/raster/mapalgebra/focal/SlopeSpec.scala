/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.Angles._
import org.scalatest._

import scala.math._

class SlopeSpec extends FunSpec with Matchers {
  describe("Slope") {
    it("should calculate Slope when the targetCell is set to Data") {
      val noData = byteNODATA
      val arr = Array[Byte](
        -1,         0, 1, 1, 1,
        noData,     2, 2, 2, 2,
        1,          2, 2, 2, 2,
        1,          2, 2, 2, 2,
        1,          2, 2, 2, 2)

      val tile = ByteArrayTile(arr, 5, 5, ByteConstantNoDataCellType)
      val size = CellSize(5, 5)

      val slope = tile.slope(size, target = TargetCell.Data)

      def calculateSlope(dx: Double, dy: Double): Double =
        degrees(atan(sqrt(pow(dx, 2) + pow(dy, 2))))

      // Calculating the slope value for the (1, 1) cell

      var dx = ((1 + 4 + 2) - (-1 + 4 + 1)) / (8.0 * size.width)
      var dy = ((1 + 4 + 2) - (-1 + 1)) / (8.0 * size.height)

      calculateSlope(dx, dy) should be (slope.getDouble(1, 1))

      // Calculating the slope value for the (2, 1) cell

      dx = ((1 + 4 + 2) - (0 + 4 + 2)) / (8.0 * size.width)
      dy = ((2 + 4 + 2) - (0 + 2 + 1)) / (8.0 * size.height)

      calculateSlope(dx, dy) should be (slope.getDouble(2, 1))

      // Calculating the slope value for the (3, 1) cell

      dx = ((1 + 4 + 2) - (1 + 4 + 2)) / (8.0 * size.width)
      dy = ((2 + 4 + 2) - (1 + 2 + 1)) / (8.0 * size.height)

      calculateSlope(dx, dy) should be (slope.getDouble(3, 1))

      // Calculating the slope value for the (4, 1) cell

      dx = ((2 + 4 + 2) - (1 + 4 + 2)) / (8.0 * size.width)
      dy = ((2 + 4 + 2) - (1 + 2 + 2)) / (8.0 * size.height)

      calculateSlope(dx, dy) should be (slope.getDouble(4, 1))
    }
  }
}
