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

package geotrellis.spark.store.slippy

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.proj4._
import geotrellis.vector._
import geotrellis.spark.testkit._

import geotrellis.spark.testkit.testfiles._

import org.scalatest._
import java.io.File

class HadoopSlippyTileWriterSpec
    extends FunSpec
    with Matchers
    with TestEnvironment
    with TestFiles
{
  describe("HadoopSlippyTileWriter") {
    val testPath = new File(outputLocalPath, "slippy-write-test").getPath

    it("can write slippy tiles") {
      val mapTransform = ZoomedLayoutScheme(WebMercator).levelForZoom(TestFiles.ZOOM_LEVEL).layout.mapTransform

      val writer =
        new HadoopSlippyTileWriter[Tile](testPath, "tif")({ (key, tile) =>
          SinglebandGeoTiff(tile, mapTransform(key), WebMercator).toByteArray
        })

      writer.write(TestFiles.ZOOM_LEVEL, AllOnesTestFile)

      val reader =
        new FileSlippyTileReader[Tile](testPath)({ (key, bytes) =>
          SinglebandGeoTiff(bytes).tile.toArrayTile
        })

      rastersEqual(reader.read(TestFiles.ZOOM_LEVEL), AllOnesTestFile)

    }
  }
}
