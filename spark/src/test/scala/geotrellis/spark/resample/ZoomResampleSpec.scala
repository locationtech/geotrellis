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

package geotrellis.spark.resample

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff.{SinglebandGeoTiff, MultibandGeoTiff}
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.vector.Extent
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class ZoomResampleMethodsSpec extends FunSpec
    with TileBuilders
    with TileLayerRDDBuilders
    with TestEnvironment {


  describe("Zoom Resample on TileLayerRDD - aspect.tif") {
    val path = "raster/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)
    val md = rdd.metadata
    val overall = md.extent
    val Extent(xmin, ymin, xmax, ymax) = overall
    val half = Extent(xmin, ymin, xmin + (xmax - xmin) / 2, ymin + (ymax - ymin) / 2)
    val small = Extent(xmin, ymin, xmin + (xmax - xmin) / 5, ymin + (ymax - ymin) / 5)

    it("should correctly crop by the rdd extent") {
      val count = rdd.crop(overall).count
      count should be (25)
    }

    it("should correctly increase the number of tiles by 2 when going up one level") {
      val resampled = rdd.resampleToZoom(5, 6)
      val count = resampled.count
      count should be (rdd.count * 4)

      val gridBounds = rdd.metadata.bounds.get.toGridBounds
      val resampledGridBounds = resampled.metadata.bounds.get.toGridBounds

      resampledGridBounds.size should be (gridBounds.size * 4)
    }
  }

  describe("Zoom Resample on MultibandTileLayerRDD - aspect.tif") {
    val path = "raster/data/aspect.tif"
    val gt = MultibandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val rdd = createMultibandTileLayerRDD(sc, originalRaster, TileLayout(5, 5, 100, 100), gt.crs)
    val md = rdd.metadata
    val overall = md.extent
    val Extent(xmin, ymin, xmax, ymax) = overall
    val half = Extent(xmin, ymin, xmin + (xmax - xmin) / 2, ymin + (ymax - ymin) / 2)
    val small = Extent(xmin, ymin, xmin + (xmax - xmin) / 5, ymin + (ymax - ymin) / 5)

    it("should correctly crop by the rdd extent") {
      val count = rdd.crop(overall).count
      count should be (25)
    }

    it("should correctly increase the number of tiles by 2 when going up one level") {
      val resampled = rdd.resampleToZoom(5, 6)
      val count = resampled.count
      count should be (rdd.count * 4)

      val gridBounds = rdd.metadata.bounds.get.toGridBounds
      val resampledGridBounds = resampled.metadata.bounds.get.toGridBounds

      resampledGridBounds.size should be (gridBounds.size * 4)
    }
  }

  describe("Zoom Resample on TileLayerRDD - manual example") {
    it("should correctly resample and filter in a larger example") {
      val tile =
        createTile(
          Array(
            1, 1,  2, 2,  3, 3,  4, 4,
            1, 1,  2, 2,  3, 3,  4, 4,

            5, 5,  6, 6,  7, 7,  8, 8,
            5, 5,  6, 6,  7, 7,  8, 8,

            9, 9,  10, 10,  11, 11,  12, 12,
            9, 9,  10, 10,  11, 11,  12, 12,

            13, 13,  13, 13,  14, 14,  15, 15,
            13, 13,  13, 13,  14, 14,  15, 15
          ), 8, 8)
      val layer =
        createTileLayerRDD(
          tile,
          TileLayout(2, 2, 4, 4)
        )

      val resampled = layer.resampleToZoom(1, 2, GridBounds(1, 1, 1, 1))
      val count = resampled.count
      count should be (1)
      val result = resampled.collect.head._2

      result.foreach { z =>
        z should be (6)
      }
    }
  }

  describe("Zoom Resample on MultibandTileLayerRDD - manual example") {
    it("should correctly resample and filter in a larger example") {
      val tile =
        createTile(
          Array(
            1, 1,  2, 2,  3, 3,  4, 4,
            1, 1,  2, 2,  3, 3,  4, 4,

            5, 5,  6, 6,  7, 7,  8, 8,
            5, 5,  6, 6,  7, 7,  8, 8,

            9, 9,  10, 10,  11, 11,  12, 12,
            9, 9,  10, 10,  11, 11,  12, 12,

            13, 13,  13, 13,  14, 14,  15, 15,
            13, 13,  13, 13,  14, 14,  15, 15
          ), 8, 8)
      val layer =
        createMultibandTileLayerRDD(
          sc,
          MultibandTile(tile, tile, tile),
          TileLayout(2, 2, 4, 4)
        )

      val resampled = layer.resampleToZoom(1, 2, GridBounds(1, 1, 1, 1))
      val count = resampled.count
      count should be (1)
      val result = resampled.collect.head._2

      result.foreach { z =>
        z should be (Array(6, 6, 6))
      }
    }
  }
}
