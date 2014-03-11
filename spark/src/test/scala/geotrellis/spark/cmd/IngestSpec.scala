/**************************************************************************
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
 **************************************************************************/

package geotrellis.spark.cmd
import geotrellis.Extent
import geotrellis.spark.TestEnvironment
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.metadata.RasterMetadata
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.storage.RasterReader
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling

import org.apache.hadoop.fs.Path
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import java.awt.image.DataBuffer

class IngestSpec extends FunSpec with TestEnvironment with ShouldMatchers {

  describe("Ingest all-ones.tif") {

    // ingest the all-ones tif
    val allOnes = new Path(inputHome, "all-ones.tif")
    val cmd = s"--input ${allOnes.toString} --output ${outputLocal}"
        
    Ingest.main(cmd.split(' '))
    val raster = new Path(outputLocal, "10")

    it("should create the correct metadata") {

      val expectedMeta = PyramidMetadata(
        Extent(141.7066666666667, -18.373333333333342, 142.56000000000003, -17.52000000000001),
        512,
        1,
        -9999.0,
        DataBuffer.TYPE_FLOAT,
        10,
        Map("10" -> new RasterMetadata(PixelExtent(0, 0, 1243, 1243), TileExtent(915, 203, 917, 206))))

      val actualMeta = PyramidMetadata(outputLocal, conf)

      actualMeta should be(expectedMeta)
    }

    it("should have the right zoom level directory") {
      localFS.exists(raster) should be(true)
    }

    it("should have the right number of splits for the base zoom level") {
      val partitioner = TileIdPartitioner(raster, conf)
      partitioner.numPartitions should be(1)
    }

    it("should have the correct tiles (checking tileIds)") {
      val meta = PyramidMetadata(outputLocal, conf)
      val reader = RasterReader(raster, conf)
      val actualTileIds = reader.map { case (tw, aw) => tw.get }.toList
      val tileExtent = meta.metadataForBaseZoom.tileExtent
      val expectedTileIds = for {
        ty <- tileExtent.ymin to tileExtent.ymax
        tx <- tileExtent.xmin to tileExtent.xmax
      } yield TmsTiling.tileId(tx, ty, meta.maxZoomLevel)
      actualTileIds should be(expectedTileIds)
      reader.close()
    }
  }
}