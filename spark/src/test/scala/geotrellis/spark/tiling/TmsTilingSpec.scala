package geotrellis.spark.tiling
import geotrellis.Extent

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class TmsTilingSpec extends FunSpec with ShouldMatchers {
  // taken from "Tile-Based Geospatial Information Systems Principles and Practices" by John T. Sample • Elias Ioup
  // for 512x512 tiles
  val tileSize = 512
  val resolutions = Array(
    0.3515625000,
    0.1757812500,
    0.0878906250,
    0.0439453125,
    0.0219726563,
    0.0109863281,
    0.0054931641,
    0.0027465820,
    0.0013732910,
    0.0006866455,
    0.0003433228,
    0.0001716614,
    0.0000858307,
    0.0000429153,
    0.0000214577,
    0.0000107288,
    0.0000053644,
    0.0000026822,
    0.0000013411,
    0.0000006706)

  describe("tile resolutions") {
    it("resolution: should spit out the right resolutions given the zoom") {
      val actual = (0 to resolutions.length).map(zoom => TmsTiling.resolution(zoom + 1, tileSize))
      (actual zip resolutions).foreach(t => t._1 should be(t._2 plusOrMinus TmsTiling.Epsilon))
    }

    it("zoom: should spit out the right zoom given the resolution") {
      resolutions.map(TmsTiling.zoom(_, tileSize)).zipWithIndex.foreach(t => t._1 should be(t._2 + 1))
    }
  }

  describe("tile extents") {
    it("latLonToTile: should yield correct tile Ids for given lat/lon coordinates") {
      for (zoom <- 1 to TmsTiling.MaxZoomLevel) {

        // low left corner tile 
        val tileLowerLeft = TmsTiling.latLonToTile(-90.0, -180.0, zoom, tileSize)
        tileLowerLeft.tx should be(0)
        tileLowerLeft.ty should be(0)

        // the upper corner tile. note that 90/180 maps to non-existent tile (numXTiles,numYTiles), 
        // so for example, tile (2,1) for zoom level 1. so we use lon/lat slightly less than 90/180
        val tileUpperRight = TmsTiling.latLonToTile(89.99999, 179.99999, zoom, tileSize)
        tileUpperRight.tx should be(TmsTiling.numXTiles(zoom) - 1)
        tileUpperRight.ty should be(TmsTiling.numYTiles(zoom) - 1)

        // tile corresponding to center point
        val tileCenter = TmsTiling.latLonToTile(0.0, 0.0, zoom, tileSize);
        tileCenter.tx should be((TmsTiling.numXTiles(zoom) / 2).toLong)
        tileCenter.ty should be((TmsTiling.numYTiles(zoom) / 2).toLong)
      }
    }

    it("extentToTile: should yield correct tile extent given lat/lon extent") {
      for (zoom <- 1 to TmsTiling.MaxZoomLevel) {
        val extent = Extent(-180.0, -90.0, // low left corner 
          179.99999, 89.99999) // upper right corner
        val tileExtent = TmsTiling.extentToTile(extent, zoom, tileSize)
        tileExtent.xmin should be(0)
        tileExtent.ymin should be(0)
        tileExtent.xmax should be(TmsTiling.numXTiles(zoom) - 1)
        tileExtent.ymax should be(TmsTiling.numYTiles(zoom) - 1)

      }
    }

  }
}