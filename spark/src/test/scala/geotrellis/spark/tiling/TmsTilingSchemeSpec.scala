package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.proj4._
import geotrellis.vector._

import org.scalatest._

class TmsTilingSchemeSpec extends FunSpec with Matchers {
  describe("TmsTilingScheme") { 
    it("Cuts up the world in two for lowest zoom level") {
      val scheme = TmsTilingScheme(Extent(0.0, 0.0, 2.0, 2.0))
      val zl = scheme.zoomLevel(0)

      zl.tileExtent(scheme.extent).tileIds.size should be (2)
      zl.tileExtent(Extent(0.25, 0.5, 0.75, 1.5)).tileIds.size should be (1)
      zl.tileExtent(Extent(1.25, 0.5, 1.75, 1.5)).tileIds.size should be (1)
    }
  }
}
