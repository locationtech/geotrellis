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

package geotrellis.spark.crop

import geotrellis.raster._
import geotrellis.raster.crop.Crop.{Options => CropOptions}
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.spark._
import geotrellis.vector.Extent
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class TileLayerRDDCropMethodsSpec extends FunSpec with TestEnvironment {

  describe("TileLayerRDD Crop Methods") {
    val path = "raster/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)
    val md = rdd.metadata
    val overall = md.extent
    val Extent(xmin, ymin, xmax, ymax) = overall
    val half = Extent(xmin, ymin, xmin + (xmax - xmin) / 2, ymin + (ymax - ymin) / 2)
    val small = Extent(xmin, ymin, xmin + (xmax - xmin) / 5, ymin + (ymax - ymin) / 5)
    val shifted = Extent(xmin + overall.width / 2, ymin + overall.height / 2, xmax + overall.width / 2, ymax + overall.height / 2)

    it("should correctly crop by the rdd extent") {
      val count = rdd.crop(overall).count
      count should be (25)
    }

    it("should correctly crop by an extent half the area of the rdd extent") {
      val cropped = rdd.crop(half)
      val count = cropped.count
      count should be (9)

      val gb = cropped.metadata.bounds.get.toGridBounds
      gb.width * gb.height should be (9)
    }

    it("should correctly crop by a small extent") {
      val cropped = rdd.crop(small)
      val count = cropped.count
      count should be (1)

      val gb = cropped.metadata.bounds.get.toGridBounds
      gb.width * gb.height should be (1)
    }

    it("should correctly crop by a shifted extent") {
      val cropped = rdd.crop(shifted)
      val count = cropped.count
      count should be (9)

      val gb = cropped.metadata.bounds.get.toGridBounds
      gb.width * gb.height should be (9)
    }

    it("should correctly crop by a shifted extent (clamp = false)") {
      val cropped = rdd.crop(shifted)
      val stitched = cropped.stitch.tile

      val croppednc = rdd.crop(shifted, CropOptions(clamp = false))
      val stitchednc = croppednc.stitch.tile

      assertEqual(stitched, stitchednc)
    }
  }
}
