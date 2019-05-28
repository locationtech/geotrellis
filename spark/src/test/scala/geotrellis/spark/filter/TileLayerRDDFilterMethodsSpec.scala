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

package geotrellis.spark.filter

import geotrellis.proj4.LatLng
import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.raster.{FloatConstantNoDataCellType, GridBounds, TileLayout}
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.layers._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class TileLayerRDDFilterMethodsSpec extends FunSpec with TestEnvironment {

  describe("SpaceTime TileLayerRDD Filter Methods") {
    val rdd = sc.parallelize(List(
      (SpaceTimeKey(0, 0, 1), true),
      (SpaceTimeKey(0, 1, 2), true),
      (SpaceTimeKey(1, 0, 2), true),
      (SpaceTimeKey(1, 1, 3), true),
      (SpaceTimeKey(0, 0, 3), true),
      (SpaceTimeKey(0, 1, 3), true),
      (SpaceTimeKey(1, 0, 4), true),
      (SpaceTimeKey(1, 1, 4), true),
      (SpaceTimeKey(0, 0, 4), true),
      (SpaceTimeKey(0, 1, 4), true)))
    val metadata = {
      val cellType = FloatConstantNoDataCellType
      val crs = LatLng
      val tileLayout = TileLayout(8, 8, 3, 4)
      val mapTransform = MapKeyTransform(crs, tileLayout.layoutDimensions)
      val gridBounds = GridBounds(1, 1, 6, 7)
      val extent = mapTransform(gridBounds)
      TileLayerMetadata(cellType, LayoutDefinition(crs.worldExtent, tileLayout), extent, crs, KeyBounds(SpaceTimeKey(0, 0, 1), SpaceTimeKey(1, 1, 4)))
    }
    val tileLayerRdd = ContextRDD(rdd, metadata)

    it("should filter out all items that are not at the given instant") {
      tileLayerRdd.toSpatial(0).count should be (0)
      tileLayerRdd.toSpatial(1).count should be (1)
      tileLayerRdd.toSpatial(2).count should be (2)
      tileLayerRdd.toSpatial(3).count should be (3)
      tileLayerRdd.toSpatial(4).count should be (4)
    }

    it ("should produce an RDD whose keys are of type SpatialKey") {
      val spatial = tileLayerRdd.toSpatial(1)
      spatial.first._1 should be (SpatialKey(0,0))
    }

    it ("should obliviously drop the temporal dimension when requested to do so (non-unique)") {
      val spatial = tileLayerRdd.toSpatial()
      spatial.count should be (10)
    }

    it ("should obliviously drop the temporal dimension when requested to do so (unique)") {
      val spatial = tileLayerRdd.toSpatialReduce((a, b) => a)
      spatial.count should be (4)
    }

  }

  describe("Spatial TileLayerRDD Filter Methods") {
    val path = "raster/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.raster.mapTile(_.toArrayTile).resample(500, 500)
    val (_, rdd) = createTileLayerRDD(originalRaster, 5, 5, gt.crs)
    val temporalRdd =
      rdd
        .withContext { _.map { case (k, v) => SpaceTimeKey(k, TemporalKey(1000l)) -> v } }
        .mapContext { md => md.copy(bounds = md.bounds match {
          case KeyBounds(minKey, maxKey) => KeyBounds(SpaceTimeKey(minKey, TemporalKey(1000l)), SpaceTimeKey(maxKey, TemporalKey(1000l)))
          case _ => EmptyBounds
        }) }
    val allKeys = KeyBounds(SpatialKey(0,0), SpatialKey(4,4))
    val someKeys = KeyBounds(SpatialKey(1,1), SpatialKey(3,3))
    val moreKeys = KeyBounds(SpatialKey(4,4), SpatialKey(4,4))
    val noKeys = KeyBounds(SpatialKey(5,5), SpatialKey(6,6))

    it("should correctly filter by a covering range") {
      val filteredRdd = rdd.filterByKeyBounds(List(allKeys))
      filteredRdd.count should be (25)
    }

    it("should correctly filter by an intersecting range") {
      val filteredRdd = rdd.filterByKeyBounds(List(KeyBounds(SpatialKey(2, 2), SpatialKey(5, 5))))
      filteredRdd.count should be (9)
      filteredRdd.metadata.bounds.get should be (KeyBounds(SpatialKey(2, 2), SpatialKey(4, 4)))
    }

    it("should correctly filter by an intersecting range given as a singleton") {
      val filteredRdd = rdd.filterByKeyBounds(someKeys)
      filteredRdd.count should be (9)
    }

    it("should correctly filter by a non-intersecting range") {
      val filteredRdd = rdd.filterByKeyBounds(List(noKeys))
      filteredRdd.count should be (0)
    }

    it("should correctly filter by multiple ranges") {
      val filteredRdd = rdd.filterByKeyBounds(List(someKeys, moreKeys, noKeys))
      filteredRdd.count should be (10)
    }

    it("should filter query by extent") {
      val md = rdd.metadata
      val Extent(xmin, ymin, xmax, ymax) = md.extent
      val half = Extent(xmin, ymin, xmin + (xmax - xmin) / 2, ymin + (ymax - ymin) / 2)

      val filteredRdd = rdd.filter().where(Intersects(half)).result

      val count = filteredRdd.count
      count should be (9)

      val gb = filteredRdd.metadata.bounds.get.toGridBounds
      gb.width * gb.height should be (9)
    }

    it("should filter query by point") {
      val md = rdd.metadata
      val point = md.extent.center
      val filteredRdd = rdd.filter().where(Contains(point)).result

      val count = filteredRdd.count
      count should be (1)

      val gb = filteredRdd.metadata.bounds.get.toGridBounds

      md.mapTransform(gb).center should be (md.extent.center)

      val filteredViaIntersects = rdd.filter().where(Intersects(point)).result
      filteredViaIntersects.count should be (1)
    }

    it("should filter query by point (temporal)") {
      val md = temporalRdd.metadata
      val point = md.extent.center
      val filteredRdd = temporalRdd.filter().where(Contains(point)).result

      val count = filteredRdd.count
      count should be (1)

      val gb = filteredRdd.metadata.bounds.get.toGridBounds

      md.mapTransform(gb).center should be (md.extent.center)

      val filteredViaIntersects = rdd.filter().where(Intersects(point)).result
      filteredViaIntersects.count should be (1)
    }
  }
}
