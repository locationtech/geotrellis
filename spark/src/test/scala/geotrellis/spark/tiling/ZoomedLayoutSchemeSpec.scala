package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.proj4._
import geotrellis.vector._

import org.scalatest._

class ZoomedLayoutSchemeSpec extends FunSpec with Matchers {
  describe("ZoomedLayoutScheme") { 
    it("Cuts up the world in two for lowest zoom level") {
      val LayoutLevel(_, tileLayout) = ZoomedLayoutScheme().levelFor(1)
      tileLayout.layoutCols should be (2)
      tileLayout.layoutRows should be (2)
    }
  }
}
