/***
 * Copyright (c) 2014 Digital Globe.
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
 ***/

package geotrellis.spark.metadata

import geotrellis.RasterExtent
import geotrellis.raster.TileLayout
import geotrellis.spark.SharedSparkContext
import geotrellis.spark.TestEnvironment
import geotrellis.spark.rdd.RasterRDD
import geotrellis.spark.testfiles.AllOnes
import geotrellis.spark.tiling.TmsTiling

import org.apache.hadoop.fs.Path
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

/* 
 * This is not just "oldMeta should be(newMeta)" since newMeta is actually supposed to be 
 * different from oldMeta in two respects:
 * 1. newMeta.extent != oldMeta.extent
 * 2. newMeta.metadataForBaseZoom.pixelExtent != oldMeta.metadataForBaseZoom.pixelExtent
 */
trait MetadataMatcher extends ShouldMatchers {
  def shouldBe(oldMeta: PyramidMetadata, newMeta: PyramidMetadata) = {
    val oldTileSize = oldMeta.tileSize
    val oldZoom = oldMeta.maxZoomLevel
    val oldTe = oldMeta.metadataForBaseZoom.tileExtent
    val oldE = TmsTiling.tileToExtent(oldTe, oldZoom, oldTileSize)
    val oldPe = TmsTiling.extentToPixel(oldE, oldZoom, oldTileSize)
    newMeta.extent should be(oldE)
    newMeta.tileSize should be(oldTileSize)
    newMeta.bands should be(PyramidMetadata.MaxBands)
    newMeta.awtRasterType should be(oldMeta.awtRasterType)
    newMeta.maxZoomLevel should be(oldZoom)

    // we can't just use meta.metadataForBaseZoom since pixelExtent there corresponds to the 
    // original image and not those of the tile boundaries
    newMeta.rasterMetadata should be(Map(oldZoom.toString -> RasterMetadata(oldPe, oldTe)))

  }
}

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

class ContextSaveSpec extends FunSpec with TestEnvironment with SharedSparkContext with MetadataMatcher {
  val allOnes = AllOnes(inputHome, conf)

  describe("Passing Context through operations tests") {
    it("should produce the expected PyramidMetadata") {
      val ones = RasterRDD(allOnes.path, sc)
      val twos = ones + ones
      val twosPath = new Path(outputLocal, ones.opCtx.zoom.toString)
      mkdir(twosPath)
      twos.save(twosPath)

      val newMeta = PyramidMetadata(outputLocal, conf)
      shouldBe(allOnes.meta, newMeta)    
    }
  }
}
