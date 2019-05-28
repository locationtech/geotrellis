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

package geotrellis.spark.stitch

import geotrellis.layers.TileLayerMetadata
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.vector.Extent
import org.scalatest.FunSpec

class RDDStitchMethodsSpec extends FunSpec
    with TileBuilders
    with TileLayerRDDBuilders
    with TestEnvironment {

  describe("Stitching spatial rdds") {
    it("should correctly stitch back together single band tile rdd") {
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

      assertEqual(tile, layer.stitch.tile)
    }

    it("should correctly stitch back together multi band tile rdd") {
      val tile1 =
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

      val tile2 =
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
          ).map(_ * 10), 8, 8)

      val tile = ArrayMultibandTile(tile1, tile2)

      val layer =
        createMultibandTileLayerRDD(
          tile,
          TileLayout(2, 2, 4, 4)
        )

      assertEqual(tile, layer.stitch.tile)
    }

    it("should reconstitute a tiled raster to its original size") {
      import geotrellis.spark.stitch.Implicits.withSpatialTileRDDMethods
      val tile: Tile = byteRaster
      val offset = 10
      val scale = 10
      val size = 5

      val extent = Extent(
        offset, offset, offset + tile.cols * scale, offset + tile.rows * scale
      )
      val praster = ProjectedRaster(tile, extent, LatLng)
      assert(praster.raster.cellSize == CellSize(scale, scale))

      val layout = LayoutDefinition(praster.raster.rasterExtent, size, size)
      val kb = KeyBounds(
        SpatialKey(0, 0), SpatialKey(layout.layoutCols, layout.layoutRows)
      )
      val tlm = TileLayerMetadata(
        praster.tile.cellType, layout, praster.extent, praster.crs, kb)

      val rdd = sc.makeRDD(Seq((praster.projectedExtent, praster.tile)))

      val tiled = TileLayerRDD(rdd.tileToLayout(tlm), tlm)
      val restitched: Tile = withSpatialTileRDDMethods(tiled).stitch().crop(tile.cols, tile.rows)

      assert(restitched.toArray() === tile.toArray(),
        s"""Expected:
           |${praster.tile.asciiDraw()}
           |
           |Stitched:
           |${restitched.asciiDraw()}
          """.stripMargin
      )
    }

    it("should allow stitch RDD of unequally-dimensioned tiles") {
      val tiles = sc.parallelize(Array(
        (SpatialKey(10,31), IntArrayTile.ofDim( 5, 5).map{ (x, y, _) => math.max(x,    y) }),
        (SpatialKey(11,31), IntArrayTile.ofDim(15, 5).map{ (x, y, _) => math.max(x+5,  y) }),
        (SpatialKey(12,31), IntArrayTile.ofDim( 7, 5).map{ (x, y, _) => math.max(x+20, y) }),
        (SpatialKey(10,32), IntArrayTile.ofDim( 5,15).map{ (x, y, _) => math.max(x,    y+5) }),
        (SpatialKey(11,32), IntArrayTile.ofDim(15,15).map{ (x, y, _) => math.max(x+5,  y+5) }),
        (SpatialKey(12,32), IntArrayTile.ofDim( 7,15).map{ (x, y, _) => math.max(x+20, y+5) }),
        (SpatialKey(10,33), IntArrayTile.ofDim( 5, 7).map{ (x, y, _) => math.max(x,    y+20) }),
        (SpatialKey(11,33), IntArrayTile.ofDim(15, 7).map{ (x, y, _) => math.max(x+5,  y+20) }),
        (SpatialKey(12,33), IntArrayTile.ofDim( 7, 7).map{ (x, y, _) => math.max(x+20, y+20) })
      ))
      val reference = IntArrayTile.ofDim(27,27).map{ (x, y, _) => math.max(x, y) }

      assertEqual(tiles.stitch, reference)
    }
  }
}
