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
import geotrellis.raster.testkit._
import geotrellis.vector.Extent
import org.scalatest._

import scala.math._

class AspectSpec extends FunSpec with Matchers with RasterMatchers with TileBuilders with TestFiles {
  describe("Aspect") {
    it("should match gdal computed aspect raster") {
      val elevation = loadTestArg("data/elevation")
      val aspect = loadTestArg("data/aspect")
      val rasterExtentElevation = elevation.rasterExtent

      //Use the testkit to resolve everything to bare Tiles
      val r: Tile = elevation.tile
      val rg: Tile = aspect.tile

      val aspectComputed = r.aspect(rasterExtentElevation.cellSize)

      val rasterExtent = aspect.rasterExtent

      // Gdal actually computes the parimeter values differently.
      // So take out the edge results
      val (xmin, ymax) = rasterExtent.gridToMap(1, 1)
      val (xmax, ymin) = rasterExtent.gridToMap(rg.cols - 2, rg.rows - 2)

      val cropExtent = Extent(xmin, ymin, xmax, ymax)

      val rgc = rg.convert(DoubleConstantNoDataCellType).crop(rasterExtent.extent, cropExtent)
      val rc = aspectComputed.crop(rasterExtentElevation.extent, cropExtent)

      // '1.1' is because gdal's flat area is 0, and our flat area is -1
      assertEqual(rgc, rc, 1.1)
    }

    it("should calculate edge cases correctly") {
      val r = createTile(
        Array[Int](
          -1,0,1,1,1,
          1,2,2,2,2,
          1,2,2,2,2,
          1,2,2,2,2,
          1,2,2,1,2), 5, 5)

      val aR = r.aspect(CellSize(5, 5))

      // Check left edge
      var value = aR.getDouble(0, 1)

      var dx = ((0 - 1) + 2 * (2 - 1) + (2 - 1)) / 8.0
      var dy = ((1 - 1) + 2 * (1 - (-1)) + (2 - 0)) / 8.0

      var aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      if(aspect < 0.0) { aspect += 360 }
      (value - aspect) should be < 0.0000001

      //Check right edge
      value = aR.getDouble(4, 1)

      dx = ((2 - 1) + 2 * (2 - 2) + (2 - 2)) / 8.0
      dy = ((2 - 1) + 2 * (2 - 1) + (2 - 2)) / 8.0

      aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      if(aspect < 0.0) { aspect += 360 }
      (value - aspect) should be < 0.0000001

      //Check bottom edge
      value = aR.getDouble(1, 4)

      dx = ((2 - 1) + 2 * (2 - 1) + (2 - 2)) / 8.0
      dy = ((2 - 1) + 2 * (2 - 2) + (2 - 2)) / 8.0

      aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      if(aspect < 0.0) { aspect += 360 }
      (value - aspect) should be < 0.0000001

      //Check top edge
      value = aR.getDouble(3, 0)

      dx = ((1 - 1) + 2 * (1 - 1) + (2 - 2)) / 8.0
      dy = ((2 - 1) + 2 * (2 - 1) + (2 - 1)) / 8.0

      aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      (value - aspect) should be < 0.0000001

      //Check top right corner 
      value = aR.getDouble(4, 0)

      dx = ((1 - 1) + 2 * (1 - 1) + (1 - 2)) / 8.0
      dy = ((2 - 1) + 2 * (2 - 1) + (1 - 1)) / 8.0

      aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      (value - aspect) should be < 0.0000001

      //Check top left corner
      value = aR.getDouble(0, 0)

      dx = (((-1) - (-1)) + 2 * (0 - (-1)) + (2 - (-1))) / 8.0
      dy = (((-1) - (-1)) + 2 * (1 - (-1)) + (2 - (-1))) / 8.0

      aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      if(aspect < 0.0) { aspect += 360 }
      (value - aspect) should be < 0.0000001

      //Check bottom left corner
      value = aR.getDouble(0, 4)

      dx = ((2 - 1) + 2 * (2 - 1) + (1 - 1)) / 8.0
      dy = ((1 - 1) + 2 * (1 - 1) + (1 - 2)) / 8.0

      aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      if(aspect < 0.0) { aspect += 360 }
      (value - aspect) should be < 0.000001

      //Check bottomr right corner
      value = aR.getDouble(4, 4)

      dx = ((2 - 2) + 2 * (2 - 1) + (2 - 2)) / 8.0
      dy = ((2 - 2) + 2 * (2 - 2) + (2 - 2)) / 8.0

      aspect = atan2(dy, dx) / (Pi / 180.0) - 90.0
      if(aspect < 0.0) { aspect += 360 }
      (value - aspect) should be < 0.0000001
    }

    it("should start from due north and be clockwise") {
      val r = createTile(
        Array[Int](
          1, 2, 3, 2, 1,
          2, 3, 4, 3, 2,
          3, 4, 5, 4, 3,
          2, 3, 4, 3, 2,
          1, 2, 3, 2, 1), 5, 5)

      val aR = r.aspect(CellSize(5, 5))

      var value = aR.getDouble(3, 1)
      (value - 45.0) should be < 0.000001

      value = aR.getDouble(3, 3)
      (value - 135.0) should be < 0.000001

      value = aR.getDouble(1, 3)
      (value - 225.0) should be < 0.000001

      value = aR.getDouble(1, 1)
      (value - 315.0) should be < 0.000001
    }

    it("should assign flat area to -1") {
      val r = createTile(
        Array[Int](
          1, 1, 1,
          1, 1, 1,
          1, 1, 1), 3, 3)

      val aR = r.aspect(CellSize(5, 5))

      var value = aR.getDouble(1, 1)
      (value - (-1.0)) should be < 0.000001
    }
  }
}
