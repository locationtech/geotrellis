/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.regrid

import geotrellis.layers.TileLayerMetadata
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.spark.tiling._
import geotrellis.vector._
import org.scalatest._

class RegridSpec extends FunSpec with TestEnvironment with RasterMatchers {

  val simpleLayer = {
    val tiles =
      for ( x <- 0 to 3 ;
            y <- 0 to 2
      ) yield {
        val tile = IntArrayTile.ofDim(32, 32)
        (SpatialKey(x, y), tile.map{ (tx, ty, _) => math.max(tx + 32 * x, ty + 32 * y) })
      }
    val rdd = sc.parallelize(tiles)
    val ex = Extent(0,0,12.8,9.6)
    val ld = LayoutDefinition(GridExtent[Long](ex, CellSize(0.1, 0.1)), 32, 32)
    val md = TileLayerMetadata[SpatialKey](IntConstantNoDataCellType,
                                           ld,
                                           ex,
                                           LatLng,
                                           KeyBounds[SpatialKey](SpatialKey(0,0), SpatialKey(3,2)))
    ContextRDD(rdd, md)
  }

  val temporalLayer = {
    val tiles =
      for ( x <- 0 to 3 ;
            y <- 0 to 2
      ) yield {
        val tile = IntArrayTile.ofDim(32, 32)
        (SpaceTimeKey(x, y, 0L), tile.map{ (tx, ty, _) => math.max(tx + 32 * x, ty + 32 * y) })
      }
    val rdd = sc.parallelize(tiles)
    val ex = Extent(0,0,12.8,9.6)
    val ld = LayoutDefinition(GridExtent[Long](ex, CellSize(0.1, 0.1)), 32, 32)
    val md = TileLayerMetadata[SpaceTimeKey](IntConstantNoDataCellType,
                                             ld,
                                             ex,
                                             LatLng,
                                             KeyBounds[SpaceTimeKey](SpaceTimeKey(0,0,0L), SpaceTimeKey(3,2,0L)))
    ContextRDD(rdd, md)
  }


  describe("Regridding") {
    it("should allow chipping into smaller tiles") {
      val newLayer = simpleLayer.regrid(16)

      assert(simpleLayer.stitch.dimensions == newLayer.stitch.dimensions)
      assertEqual(simpleLayer.stitch, newLayer.stitch)
    }

    it("should allow joining into larger tiles") {
      val newLayer = simpleLayer.regrid(64)

      assert(newLayer.stitch.dimensions == (128, 128))
      assertEqual(simpleLayer.stitch.tile, newLayer.stitch.tile.crop(0,0,127,95))
    }

    it("should allow breaking into non-square tiles") {
      val newLayer = simpleLayer.regrid(50, 25)

      assert(newLayer.stitch.dimensions == (150, 100))
      assertEqual(simpleLayer.stitch.tile, newLayer.stitch.tile.crop(0,0,127,95))
    }

    it("should work for spatiotemporal data") {
      val newLayer = temporalLayer.regrid(50, 25)

      assert(newLayer.toSpatial(0L).stitch.dimensions == (150, 100))
      assertEqual(temporalLayer.toSpatial(0L).stitch.tile, newLayer.toSpatial(0L).stitch.tile.crop(0,0,127,95))
    }
  }

}
