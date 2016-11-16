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

package geotrellis.spark.buffer

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.vector._
import geotrellis.raster.dem._

import org.scalatest.FunSpec


class BufferTilesSpec extends FunSpec with TestEnvironment {

  describe("General BufferTiles functionality") {
    it("should union neighbors, not union non-neighbors") {
      val key1 = SpatialKey(0,0)
      val key2 = SpatialKey(1,1)
      val key3 = SpatialKey(13, 33)
      val cloud1 = PointCloud(Array(Point(0.5,0.5)), Array(0))
      val cloud2 = PointCloud(Array(Point(1.5,1.5)), Array(1))
      val cloud3 = PointCloud(Array[Point](), Array[Int]())

      val rdd = sc.parallelize(List((key1, cloud1), (key2, cloud2), (key3, cloud3)))
      val results = BufferTiles(rdd).map({ case (_, cloud) =>
        cloud
          .records
          .getOrElse(Z, throw new Exception)
          .asInstanceOf[IntegralLasRecord]
          .data.length })
        .collect

      results(0) should be (2)
      results(1) should be (2)
      results(2) should be (0)
    }
  }

  describe("The BufferTiles functionality") {
    val path = "raster-test/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)
    val (_, wholeRdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)
    val metadata = wholeRdd.metadata
    val wholeCollection = wholeRdd.toCollection
    val cmetadata = wholeCollection.metadata

    it("should work when the RDD is a diagonal strip") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col == k.row }), metadata)
      BufferTiles(partialRdd, 1).count
    }

    it("should work when the RDD is a square minus the main diagonal") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col != k.row }), metadata)
      BufferTiles(partialRdd, 1).count
    }

    it("should work when the RDD is the other diagonal strip") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col == (4- k.row) }), metadata)
      BufferTiles(partialRdd, 1).count
    }

    it("should work when the RDD is a square minus the other diagonal") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col != (4- k.row) }), metadata)
      BufferTiles(partialRdd, 1).count
    }

    it("should work when the Collection is a diagonal strip") {
      val partialCollection = ContextCollection(wholeCollection.filter({ case (k, _) => k.col == k.row }), cmetadata)
      BufferTiles(partialCollection, 1).length
    }

    it("should work when the Collection is a square minus the main diagonal") {
      val partialCollection = ContextCollection(wholeCollection.filter({ case (k, _) => k.col != k.row }), cmetadata)
      BufferTiles(partialCollection, 1).length
    }

    it("should work when the Collection is the other diagonal strip") {
      val partialCollection = ContextCollection(wholeCollection.filter({ case (k, _) => k.col == (4- k.row) }), cmetadata)
      BufferTiles(partialCollection, 1).length
    }

    it("should work when the Collection is a square minus the other diagonal") {
      val partialCollection = ContextCollection(wholeCollection.filter({ case (k, _) => k.col != (4- k.row) }), cmetadata)
      BufferTiles(partialCollection, 1).length
    }
  }
}
