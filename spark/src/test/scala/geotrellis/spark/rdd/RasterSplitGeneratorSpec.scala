/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.rdd

import geotrellis.spark.tiling._
import geotrellis.raster._

import org.scalatest._

class RasterSplitGeneratorSpec extends FunSpec with Matchers {
  describe("RasterSplitGenerator") {
    it("should yield an increment of (-1, -1) (no splits) if tilesPerBlock >= tileCount") {
      val LayoutLevel(_, tileLayout) = TilingScheme.TMS.level(1)
      val indexGridTransform = RowIndexScheme(tileLayout.tileCols, tileLayout.tileRows)

      // tilesPerBlock == tileCount
      RasterSplitGenerator.computeIncrement(GridBounds(0, 0, 1, 1), 1, 4) should be((-1, -1))
      RasterSplitGenerator(GridBounds(0, 0, 1, 1), indexGridTransform, 1, 4).splits should be(Seq.empty)

      // tilesPerBlock > tileCount
      RasterSplitGenerator.computeIncrement(GridBounds(0, 0, 1, 1), 1, 5) should be((-1, -1))
      RasterSplitGenerator(GridBounds(0, 0, 1, 1), indexGridTransform, 1, 5).splits should be(Seq.empty)
    }

    it("should yield an increment of 2 tiles per split if tileExtent.width > tilesPerBlock") {
      val LayoutLevel(_, tileLayout) = TilingScheme.TMS.level(3)
      val indexGridTransform = RowIndexScheme(tileLayout.tileCols, tileLayout.tileRows)

      /* 
       * the tile ids look like
       * 	32	33	34	35	36 
       *	24	25	26	27	28
       * 	16	17	18	19	20
       *    8	9	10	11	12
       *    0	1 	2	3	4
       * If the x increments are 2, then one should expect the below split points
       * Note that the compressionFactor isn't playing a role here because of rounding to Int
       */

      RasterSplitGenerator.computeIncrement(GridBounds(0, 0, 4, 4), 1, 2) should be((2, 1))

      val splits = 
        RasterSplitGenerator(GridBounds(0, 0, 4, 4), indexGridTransform, 1, 2).splits 
      splits should be(Seq(1,3,4,9,11,12,17,19,20,25,27,28,33,35))
    }

    it("should yield an increment of > 1 (row per split) if tileCount > tilesPerBlock >= tileExtent.width") {
      RasterSplitGenerator.computeIncrement(GridBounds(0, 0, 2, 2), 1, 6) should be((-1, 2))
    }
  }
}
