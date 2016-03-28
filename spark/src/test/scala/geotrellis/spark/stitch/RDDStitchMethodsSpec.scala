package geotrellis.spark.stitch

import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
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
  }
}
