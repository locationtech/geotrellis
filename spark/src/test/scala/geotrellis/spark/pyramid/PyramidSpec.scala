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

package geotrellis.spark.pyramid

import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.stitch._
import geotrellis.spark.tiling._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark.testkit._
import jp.ne.opt.chronoscala.Imports._
import java.time.{ZoneOffset, ZonedDateTime}

import geotrellis.layers.TileLayerMetadata
import org.scalatest._

class PyramidSpec extends FunSpec with Matchers with TestEnvironment {

  describe("Pyramid") {
    it("should work with SpaceTimeKey rasters") {
      val tileLayout = TileLayout(4, 4, 2, 2)

      val dt1 = ZonedDateTime.of(2014, 5, 17, 4, 0, 0, 0, ZoneOffset.UTC)
      val dt2 = ZonedDateTime.of(2014, 5, 18, 3, 0, 0, 0, ZoneOffset.UTC)

      val tile1 =
        ArrayTile(Array(
          1, 1,  1, 1,   2, 2,  2, 2,
          1, 1,  1, 1,   2, 2,  2, 2,

          1, 1,  1, 1,   2, 2,  2, 2,
          1, 1,  1, 1,   2, 2,  2, 2,


          3, 3,  3, 3,   4, 4,  4, 4,
          3, 3,  3, 3,   4, 4,  4, 4,

          3, 3,  3, 3,   4, 4,  4, 4,
          3, 3,  3, 3,   4, 4,  4, 4
        ) , 8, 8)

      val tile2 =
        ArrayTile(Array(
          10, 10,  10, 10,   20, 20,  20, 20,
          10, 10,  10, 10,   20, 20,  20, 20,

          10, 10,  10, 10,   20, 20,  20, 20,
          10, 10,  10, 10,   20, 20,  20, 20,


          30, 30,  30, 30,   40, 40,  40, 40,
          30, 30,  30, 30,   40, 40,  40, 40,

          30, 30,  30, 30,   40, 40,  40, 40,
          30, 30,  30, 30,   40, 40,  40, 40
        ) , 8, 8)

      val rdd =
        createSpaceTimeTileLayerRDD(
          Seq( (tile1, dt1), (tile2, dt2) ),
          tileLayout
        )

      val layoutScheme = ZoomedLayoutScheme(LatLng, 2)
      val level = layoutScheme.levelForZoom(LatLng.worldExtent, 2)

      val (levelOne, levelOneRDD) = Pyramid.up(rdd,layoutScheme, level.zoom)

      levelOneRDD.metadata.layout.tileLayout should be (TileLayout(2, 2, 2, 2))
      val results: Array[(SpaceTimeKey, Tile)] = levelOneRDD.collect()

      results.map(_._1.temporalKey.instant).distinct.sorted.toSeq should be (Seq(dt1.toInstant.toEpochMilli, dt2.toInstant.toEpochMilli))

      for((key, tile) <- results) {
        val multi =
          if(key.temporalKey.instant == dt1.toInstant.toEpochMilli) 1
          else 10
        key.spatialKey match {
          case SpatialKey(0, 0) =>
            tile.toArray.distinct should be (Array(1 * multi))
          case SpatialKey(1, 0) =>
            tile.toArray.distinct should be (Array(2 * multi))
          case SpatialKey(0, 1) =>
            tile.toArray.distinct should be (Array(3 * multi))
          case SpatialKey(1, 1) =>
            tile.toArray.distinct should be (Array(4 * multi))
        }
      }
    }

    it("should pyramid Bounds[SpatialKey]") {
      val md = TileLayerMetadata(
        ByteCellType,
        LayoutDefinition(
          Extent(-2.0037508342789244E7, -2.0037508342789244E7,
            2.0037508342789244E7, 2.0037508342789244E7),
          TileLayout(8192,8192,256,256)
        ),
        Extent(-9634947.090382002, 4024185.376428919,
          -9358467.589532925, 4300664.877277998),
        WebMercator,
        KeyBounds(SpatialKey(2126,3216),SpatialKey(2182,3273))
      )

      val scheme =  ZoomedLayoutScheme(WebMercator, 256)
      var rdd = ContextRDD(sc.emptyRDD[(SpatialKey, Tile)], md)
      var zoom: Int = 13

      while (zoom > 0) {
        val (newZoom, newRDD) = Pyramid.up(rdd, scheme, zoom)
        val previousExtent = rdd.metadata.mapTransform(rdd.metadata.bounds.get.toGridBounds())
        val nextExtent = newRDD.metadata.mapTransform(newRDD.metadata.bounds.get.toGridBounds())
        nextExtent.contains(previousExtent) should be (true)
        zoom = newZoom
        rdd = newRDD
      }
    }

    it("should pyramid floating layer") {
      val tileLayout = TileLayout(4, 4, 2, 2)

      val dt1 = ZonedDateTime.of(2014, 5, 17, 4, 0, 0, 0, ZoneOffset.UTC)
      val dt2 = ZonedDateTime.of(2014, 5, 18, 3, 0, 0, 0, ZoneOffset.UTC)

      val tile1 =
        ArrayTile(Array(
          1, 1,  1, 1,   2, 2,  2, 2,
          1, 1,  1, 1,   2, 2,  2, 2,

          1, 1,  1, 1,   2, 2,  2, 2,
          1, 1,  1, 1,   2, 2,  2, 2,


          3, 3,  3, 3,   4, 4,  4, 4,
          3, 3,  3, 3,   4, 4,  4, 4,

          3, 3,  3, 3,   4, 4,  4, 4,
          3, 3,  3, 3,   4, 4,  4, 4
        ) , 8, 8)

      val tile2 =
        ArrayTile(Array(
          10, 10,  10, 10,   20, 20,  20, 20,
          10, 10,  10, 10,   20, 20,  20, 20,

          10, 10,  10, 10,   20, 20,  20, 20,
          10, 10,  10, 10,   20, 20,  20, 20,


          30, 30,  30, 30,   40, 40,  40, 40,
          30, 30,  30, 30,   40, 40,  40, 40,

          30, 30,  30, 30,   40, 40,  40, 40,
          30, 30,  30, 30,   40, 40,  40, 40
        ) , 8, 8)

      val rdd =
        createSpaceTimeTileLayerRDD(
          Seq( (tile1, dt1), (tile2, dt2) ),
          tileLayout
        )

      val layoutScheme = ZoomedLayoutScheme(LatLng, 2)
      val level = LayoutLevel(2, FloatingLayoutScheme(512).levelFor(LatLng.worldExtent, CellSize(0.5, 0.5)).layout)

      val (levelOne, levelOneRDD) = Pyramid.up(rdd,layoutScheme, level.zoom)

      levelOneRDD.metadata.layout.tileLayout should be (TileLayout(2, 2, 2, 2))
      val results: Array[(SpaceTimeKey, Tile)] = levelOneRDD.collect()

      results.map(_._1.temporalKey.instant).distinct.sorted.toSeq should be (Seq(dt1.toInstant.toEpochMilli, dt2.toInstant.toEpochMilli))

      for((key, tile) <- results) {
        val multi =
          if(key.temporalKey.instant == dt1.toInstant.toEpochMilli) 1
          else 10
        key.spatialKey match {
          case SpatialKey(0, 0) =>
            tile.toArray.distinct should be (Array(1 * multi))
          case SpatialKey(1, 0) =>
            tile.toArray.distinct should be (Array(2 * multi))
          case SpatialKey(0, 1) =>
            tile.toArray.distinct should be (Array(3 * multi))
          case SpatialKey(1, 1) =>
            tile.toArray.distinct should be (Array(4 * multi))
        }
      }
    }

    it("should produce the expected result for pyramid levels") {
      val tileLayout = TileLayout(4, 4, 2, 2)
      val tile =
        ArrayTile(Array(
          1, 1,  1, 1,   2, 2,  2, 2,
          1, 1,  1, 1,   2, 2,  2, 2,

          1, 1,  1, 1,   2, 2,  2, 2,
          1, 1,  1, 1,   2, 2,  2, 2,


          3, 3,  3, 3,   4, 4,  4, 4,
          3, 3,  3, 3,   4, 4,  4, 4,

          3, 3,  3, 3,   4, 4,  4, 4,
          3, 3,  3, 3,   4, 4,  4, 4
        ) , 8, 8)

      val baseLayer = createTileLayerRDD(tile, tileLayout)

      // Build pyramid using Average resampling
      val pyramid = Pyramid.fromLayerRDD(baseLayer)

      // The 1 tile top of the pyramid should be set to zoom 0, base layer numbered accordingly
      assert(pyramid.minZoom == 0)
      assert(pyramid.maxZoom == 2)

      val tile2x2 = pyramid(0).stitch.tile

      // should end up with the proper top-level tile
      assert(tile2x2.dimensions == (2, 2))
      assertEqual(tile2x2, Array(1,2,3,4))
    }
  }
}
