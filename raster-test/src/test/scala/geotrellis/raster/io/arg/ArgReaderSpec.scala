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

package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.testkit._

import org.scalatest._

class ArgReaderSpec extends FunSpec
                            with RasterMatchers
                            with TestFiles
                            with Matchers {
  describe("ArgReader") {
    it("should read a constant tile") {
      val tile = ArgReader.read("raster-test/data/data/constant.json").tile
      tile match {
        case ct: ConstantTile =>
          tile.cellType should be (TypeInt)
          tile.get(0,0) should be (5)
        case _ => sys.error(s"Tile should be constant tile, is actually ${tile.getClass.getSimpleName}")
      }
    }

    it("should read a constant tile with a NaN value") {
      val tile = ArgReader.read("raster-test/data/data/constant-nan.json").tile
      tile match {
        case ct: ConstantTile =>
          tile.cellType should be (TypeDouble)
          isNoData(tile.getDouble(0,0)) should be (true)
        case _ => sys.error(s"Tile should be constant tile, is actually ${tile.getClass.getSimpleName}")
      }
    }

  }
}
