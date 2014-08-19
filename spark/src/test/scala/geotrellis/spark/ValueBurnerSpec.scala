package geotrellis.spark

import org.scalatest._
import geotrellis.raster._
import geotrellis.vector._

class ValueBurnerSpec extends FunSpec with Matchers {

  val tiles = Array(
    Extent(0,4,4,8) -> IntArrayTile.fill(0,4,4),
    Extent(4,4,8,8) -> IntArrayTile.fill(1,4,4),
    Extent(0,0,4,4) -> IntArrayTile.fill(2,4,4),
    Extent(4,0,8,4) -> IntArrayTile.fill(3,4,4)
  )

  describe("ValueBurner"){
    it("should burn values from overlapping extents"){
      val extent = Extent(2,2,6,6)
      val burnTile = ArrayTile.empty(TypeInt, 4,4)

      for ( (ex, tile) <- tiles) {
        burnTile.burnValues(extent, ex, tile)
      }
      val expected = ArrayTile(Array(
        0,0,1,1,
        0,0,1,1,
        2,2,3,3,
        2,2,3,3), 4, 4)

      burnTile should equal (expected)
    }
  }
}
