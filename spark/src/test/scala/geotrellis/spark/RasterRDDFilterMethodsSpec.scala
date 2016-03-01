/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.filter._
import geotrellis.raster.io.geotiff.SingleBandGeoTiff

import org.scalatest.FunSpec


class RasterRDDFilterMethodsSpec extends FunSpec with TestEnvironment {

  describe("RasterRDD Crop Methods") {
    val path = "raster-test/data/aspect.tif"
    val gt = SingleBandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val (_, rdd) = createRasterRDD(originalRaster, 5, 5, gt.crs)
    val allKeys = KeyBounds(SpatialKey(0,0), SpatialKey(4,4))
    val someKeys = KeyBounds(SpatialKey(1,1), SpatialKey(3,3))
    val moreKeys = KeyBounds(SpatialKey(4,4), SpatialKey(4,4))
    val noKeys = KeyBounds(SpatialKey(5,5), SpatialKey(6,6))

    it("should correctly filter by a covering range") {
      val query = rdd.filterByKeyBounds(List(allKeys))
      query.toRDD.count should be (25)
    }

    it("should correctly filter by an intersecting range") {
      val query = rdd.filterByKeyBounds(List(someKeys))
      query.toRDD.count should be (9)
    }

    it("should correctly filter by an intersecting range given as a singleton") {
      val query = rdd.filterByKeyBounds(someKeys)
      query.toRDD.count should be (9)
    }

    it("should correctly filter by a non-intersecting range") {
      val query = rdd.filterByKeyBounds(List(noKeys))
      query.toRDD.count should be (0)
    }

    it("should correctly filter by multiple ranges") {
      val query = rdd.filterByKeyBounds(List(someKeys, moreKeys, noKeys))
      query.toRDD.count should be (10)
    }
  }
}

