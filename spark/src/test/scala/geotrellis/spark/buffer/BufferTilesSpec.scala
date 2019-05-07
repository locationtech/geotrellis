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

import geotrellis.tiling._
import geotrellis.raster.buffer.BufferSizes
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.raster.testkit._
import geotrellis.layers.ContextCollection
import geotrellis.layers.buffer.BufferTiles
import geotrellis.spark._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

object BufferTilesSpec {
  def generateBufferSizes(bounds: Bounds[SpatialKey])(key: SpatialKey) = {
    if (bounds includes key)
      Some(BufferSizes(2,2,2,2))
    else
      None
  }

  def generateBufferSizesFromKeys(members: Set[SpatialKey])(key: SpatialKey) = {
    if (members contains key)
      Some(BufferSizes(2,2,2,2))
    else
      None
  }
}


class BufferTilesSpec extends FunSpec with TestEnvironment with RasterMatchers {
  describe("The BufferTiles functionality") {
    val path = "raster/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.mapTile(_.toArrayTile).raster.resample(500, 500)
    val (_, wholeRdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)
    val metadata = wholeRdd.metadata
    val wholeCollection = wholeRdd.toCollection
    val cmetadata = wholeCollection.metadata

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

    it("should work when the RDD is a diagonal strip") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col == k.row }), metadata)
      BufferTilesRDD(partialRdd, 1).count
    }

    it("should work when the RDD is a square minus the main diagonal") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col != k.row }), metadata)
      BufferTilesRDD(partialRdd, 1).count
    }

    it("should work when the RDD is the other diagonal strip") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col == (4- k.row) }), metadata)
      BufferTilesRDD(partialRdd, 1).count
    }

    it("should work when the RDD is a square minus the other diagonal") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col != (4- k.row) }), metadata)
      BufferTilesRDD(partialRdd, 1).count
    }

    it("the lightweight RDD version should work for the whole collection") {
      val bounds = metadata.bounds

      val buffers = BufferTilesRDD(ContextRDD(wholeRdd, metadata), { _: SpatialKey => BufferSizes(2,2,2,2) }).collect
      val tile11 = buffers.find{ case (key, _) => key == SpatialKey(1, 1) }.get._2.tile
      val baseline = originalRaster.crop(98, 98, 201, 201, Crop.Options.DEFAULT)
      assertEqual(baseline.tile, tile11)
    }

    it("the lightweight RDD version should work with the main diagonal missing") {
      val partialRdd = ContextRDD(wholeRdd.filter({ case (k, _) => k.col != k.row }), metadata)
      val bounds = metadata.bounds
      val members = partialRdd.collect.map(_._1).toSet

      val blank = originalRaster.tile.prototype(100, 100)
      println(blank)
      val holey = originalRaster.tile.mapDouble{x => x}.mutable
      println(holey)
      for ( x <- 0 to 4 ) {
        println(s"Updating tile ($x, $x)")
        holey.update(x * 100, x * 100, blank)
      }

      val buffers = BufferTilesRDD(partialRdd, { _: SpatialKey => BufferSizes(2,2,2,2) }).collect
      val tile11 = buffers.find{ case (key, _) => key == SpatialKey(2, 1) }.get._2.tile
      println(tile11)
      // Holey crop!
      val baseline = holey.crop(198, 98, 301, 201, Crop.Options.DEFAULT)
      println(baseline)
      assertEqual(baseline, tile11)
    }
  }
}
