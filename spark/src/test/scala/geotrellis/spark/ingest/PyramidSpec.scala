package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.proj4.LatLng
import geotrellis.raster._

import com.github.nscala_time.time.Imports._
import org.scalatest._


class PyramidSpec extends FunSpec with Matchers with TestEnvironment {

  describe("Pyramid") {
    it("should work with GridTimeKey rasters") {
      val tileLayout = TileLayout(4, 4, 2, 2)

      val dt1 = new DateTime(2014, 5, 17, 4, 0)
      val dt2 = new DateTime(2014, 5, 18, 3, 0)

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
        createSpaceTimeRasterRDD(
          Seq( (tile1, dt1), (tile2, dt2) ),
          tileLayout
        )

      val layoutScheme = ZoomedLayoutScheme(LatLng, 2)
      val level = layoutScheme.levelForZoom(LatLng.worldExtent, 2)

      val (levelOne, levelOneRDD) = Pyramid.up(rdd,layoutScheme, level.zoom)

      levelOneRDD.metadata.layout.tileLayout should be (TileLayout(2, 2, 2, 2))
      val results: Array[(GridTimeKey, Tile)] = levelOneRDD.collect()

      results.map(_._1.temporalKey.instant).distinct.sorted.toSeq should be (Seq(dt1.getMillis, dt2.getMillis))

      for((key, tile) <- results) {
        val multi =
          if(key.temporalKey.instant == dt1.getMillis) 1
          else 10
        key.spatialKey match {
          case GridKey(0, 0) =>
            tile.toArray.distinct should be (Array(1 * multi))
          case GridKey(1, 0) =>
            tile.toArray.distinct should be (Array(2 * multi))
          case GridKey(0, 1) =>
            tile.toArray.distinct should be (Array(3 * multi))
          case GridKey(1, 1) =>
            tile.toArray.distinct should be (Array(4 * multi))
        }
      }
    }
  }
}
