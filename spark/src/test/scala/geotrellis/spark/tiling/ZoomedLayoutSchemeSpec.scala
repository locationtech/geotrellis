package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.vector._

import org.scalatest._

class ZoomedLayoutSchemeSpec extends FunSpec with Matchers {
  // From http://wiki.openstreetmap.org/wiki/Zoom_levels
  val zoomMeters: Map[Int, Double] =
    Map(
      0 -> 156412,	// 1:500 million (whole world)
      1 -> 78206,       // 1:250 million
      2 -> 39103,       // 1:150 million
      3 -> 19551,       // 1:70 million
      4 -> 9776,        // 1:35 million
      5 -> 4888,        // 1:15 million
      6 -> 2444,        // 1:10 million
      7 -> 1222,        // 1:4 million
      8 -> 610.984,     // 1:2 million
      9 -> 305.492,     // 1:1 million (wide area)
      10 -> 152.746,    // 1:500,000
      11 -> 76.373,     // 1:250,000 (area)
      12 -> 38.187,     // 1:150,000
      13 -> 19.093,     // 1:70,000 (village or town)
      14 -> 9.547,      // 1:35,000
      15 -> 4.773,      // 1:15,000
      16 -> 2.387,      // 1:8,000 (small road)
      17 -> 1.193,      // 1:4,000
      18 -> 0.596,      // 1:2,000
      19 -> 0.298       // 1:1,000
    )

  describe("ZoomedLayoutScheme") { 
    it("Cuts up the world in two for lowest zoom level") {
      val LayoutLevel(_, tileLayout) = ZoomedLayoutScheme(LatLng).levelForZoom(LatLng.worldExtent, 1)
      tileLayout.layoutCols should be (2)
      tileLayout.layoutRows should be (2)
    }

    it("produces known zoom level <= 10% the way towards next zoom level") {
      val wmScheme = ZoomedLayoutScheme(WebMercator, 256)

      for(zoom <- 0 to 18) {
        val diff = zoomMeters(zoom) - zoomMeters(zoom + 1)
        val m = zoomMeters(zoom) - (diff * 0.07)
        val cellSize = CellSize(m, m)

        val z = wmScheme.zoom(0, 0, cellSize)
        z should be (zoom)
      }
    }

    it("produces known zoom level > 10% the way towards next zoom level") {
      val wmScheme = ZoomedLayoutScheme(WebMercator, 256)

      for(zoom <- 0 to 18) {
        val diff = zoomMeters(zoom) - zoomMeters(zoom + 1)
        val m = zoomMeters(zoom) - (diff * 0.11)
        val cellSize = CellSize(m, m)

        val z = wmScheme.zoom(0, 0, cellSize)
        z should be (zoom + 1)
      }
    }

    it("produces known zoom level <= 20% the way towards next zoom level, 0.2 threshold") {
      val wmScheme = ZoomedLayoutScheme(WebMercator, 256, resolutionThreshold = 0.2)

      for(zoom <- 0 to 18) {
        val diff = zoomMeters(zoom) - zoomMeters(zoom + 1)
        val m = zoomMeters(zoom) - (diff * 0.17)
        val cellSize = CellSize(m, m)

        val z = wmScheme.zoom(0, 0, cellSize)
        z should be (zoom)
      }
    }

    it("produces known zoom level > 20% the way towards next zoom level, 0.2 threshold") {
      val wmScheme = ZoomedLayoutScheme(WebMercator, 256, resolutionThreshold = 0.2)

      for(zoom <- 0 to 18) {
        val diff = zoomMeters(zoom) - zoomMeters(zoom + 1)
        val m = zoomMeters(zoom) - (diff * 0.21)
        val cellSize = CellSize(m, m)

        val z = wmScheme.zoom(0, 0, cellSize)
        z should be (zoom + 1)
      }
    }
  }
}
