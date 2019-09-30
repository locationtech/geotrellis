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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class ResampleTargetSpec extends FlatSpec with Matchers {
  "Targetting dimensions" should "shrink cellsize when resampling to a larger set of cols/rows" in {
    val ge = GridExtent(Extent(0, 0, 10, 10), 10, 10)
    val target = TargetDimensions(100, 100)
    ge.cellSize.width should be (1.0)
    target(ge).cellSize.width should be (0.1)
  }

  "Targetting a grid" should "snap to target grid roughly maintaining origin extent" in {
    val ge = GridExtent(Extent(0, 0, 10, 10), 10, 10)
    val target = TargetGrid[Int](GridExtent(Extent(20, 20, 30, 30), 100, 100))
    ge.cellSize.width should be (1.0)
    target(ge).cellSize.width should be (0.1)
    target(ge).extent should be (Extent(0, 0, 10, 10))
  }

  "Targetting a gridextent" should "just use the targetted GridExtent" in {
    val ge = GridExtent(Extent(0, 0, 10, 10), 10, 10)
    val targetGe = GridExtent(Extent(20, 20, 30, 30), 100, 100)
    val target = TargetGridExtent[Int](targetGe)
    target(ge) should be (targetGe)
  }

  "Targetting a cellsize" should "decrease (stepwise) cols/rows as the cellsize increases" in {
    val ge = GridExtent(Extent(0, 0, 100, 100), 10, 10)
    val target = TargetCellSize(CellSize(11, 11))
    ge.cols should be (10)
    target(ge).cols should be < (10)
  }
}
