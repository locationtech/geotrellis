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

package geotrellis.spark.metadata

import geotrellis.raster.RasterExtent
import geotrellis.raster.TileLayout
import geotrellis.spark.TestEnvironment
import geotrellis.spark.testfiles.AllOnes
import geotrellis.spark.tiling.TmsTiling
import org.scalatest._

/*
 * This is split into two classes as one mixes in TestEnvironmentFixture and the other mixes 
 * in TestEnvironment
 */ 
class ContextSpec extends FunSpec with TestEnvironment with MetadataMatcher {
  val allOnes = AllOnes(inputHome, conf)
  val meta = allOnes.meta
  val te = meta.metadataForBaseZoom.tileExtent
  val layout = allOnes.tileLayout
  val rasterExtent = allOnes.rasterExtent
  val zoom = meta.maxZoomLevel
  val tileSize = meta.tileSize
  val res = TmsTiling.resolution(zoom, tileSize)

  describe("RasterMetadata to RasterDefinition conversion tests") {
    it("should have the correct tile layout") {
      layout should be(TileLayout(te.width.toInt, te.height.toInt, tileSize, tileSize))
    }
    it("should have the correct raster extent") {
      val e = TmsTiling.tileToExtent(te, zoom, tileSize)
      val cols = tileSize * te.width
      val rows = tileSize * te.height
      rasterExtent should be(RasterExtent(e, res, res, cols.toInt, rows.toInt))
    }
  }
  describe("RasterDefinition to RasterMetadata conversion tests") {
    it("should have the original raster metadata") {
      val newMeta = allOnes.opCtx.toMetadata
      shouldBe(meta, newMeta)
    }
  }
}