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

package geotrellis.spark.summary

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import geotrellis.raster._
import geotrellis.raster.io.geotiff._

import geotrellis.vector._

import org.scalatest.FunSpec

import collection._

class StatsTileRDDMethodsSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("RDD Stats Method Operations") {

    it("gives correct class breaks for example raster histogram") {
      val rdd = createTileLayerRDD(
        sc,
        ArrayTile(Array(
          1, 1, 1,  1, 1, 1,  1, 1, 1,
          1, 1, 1,  1, 1, 1,  1, 1, 1,

          2, 2, 2,  2, 2, 2,  2, 2, 2,
          2, 2, 2,  2, 2, 2,  2, 2, 2,

          3, 3, 3,  3, 3, 3,  3, 3, 3,
          3, 3, 3,  3, 3, 3,  3, 3, 3,

          4, 4, 4,  4, 4, 4,  4, 4, 4,
          4, 4, 4,  4, 4, 4,  4, 4, 4), 9, 8),
        TileLayout(3, 4, 3, 2)
      )

      val classBreaks = rdd.classBreaksDouble(4)

      classBreaks should be (Array(1.0, 2.0, 3.0, 4.0))
    }

    it("should find integer min/max of AllOnesTestFile") {
      val ones: TileLayerRDD[SpatialKey] = AllOnesTestFile
      val (min, max) = ones.minMax

      min should be (1)
      max should be (1)
    }

    it ("should find integer min/max of example") {
      val arr: Array[Int] =
        Array(1, 1, 2, 2,
          3, 3, 4, 4,

          -1, -1, -2, -2,
          -3, -3, -4, -4)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(sc, tile, tileLayout)

      val (min, max) = rdd.minMax

      min should be (-4)
      max should be (4)
    }

    it ("should find double min/max of example") {
      val arr: Array[Double] =
        Array(1, 1, 2, 2,
          3, 3, 4.1, 4.1,

          -1, -1, -2, -2,
          -3, -3, -4.1, -4.1)

      val tile = ArrayTile(arr, 4, 4)
      val tileLayout = TileLayout(2, 2, 2, 2)

      val rdd = createTileLayerRDD(sc, tile, tileLayout)

      val (min, max) = rdd.minMaxDouble

      min should be (-4.1)
      max should be (4.1)
    }

    it ("should find double histogram of aspect and match merged quantile breaks") {
      val path = "raster/data/aspect.tif"
      val gt = SinglebandGeoTiff(path)
      val originalRaster = gt.raster.mapTile(_.toArrayTile).resample(500, 500)
      val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)

      val hist = rdd.histogram
      val hist2 = rdd.histogram

      hist.merge(hist2).quantileBreaks(70) should be (hist.quantileBreaks(70))
    }

    it ("should be able to sample a fraction of an RDD to compute a histogram") {
      val path = "raster/data/aspect.tif"
      val gt = SinglebandGeoTiff(path)
      val originalRaster = gt.raster.mapTile(_.toArrayTile).resample(500, 500)
      val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)

      val hist1 = rdd.histogram(72)
      val hist2 = rdd.histogram(72, 1.0/25)

      hist2.totalCount.toDouble should be >= (hist1.totalCount / 25.0)
      hist2.totalCount.toDouble should be <= (hist1.totalCount / 12.5)
    }
  }
}
