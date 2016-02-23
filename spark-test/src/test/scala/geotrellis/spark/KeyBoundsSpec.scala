package geotrellis.spark

import org.scalatest._

class KeyBoundsSpec extends FunSpec with Matchers {
  val bounds1 = KeyBounds(SpatialKey(1,1), SpatialKey(3,3))
  val bounds2 = KeyBounds(SpatialKey(2,2), SpatialKey(4,4))
  val bounds3 = EmptyBounds

  it("combine with empty"){
    bounds1 combine bounds3 should be (bounds1)
    bounds3 combine bounds1 should be (bounds1)
    bounds1 combine bounds2 should be (KeyBounds(SpatialKey(1,1), SpatialKey(4,4)))
  }

  it("intersect with empty"){
    bounds1 intersect bounds3 should be (EmptyBounds)
    bounds3 intersect bounds1 should be (EmptyBounds)
  }

  it("intersects with non-empty"){
    val expected = KeyBounds(SpatialKey(2,2), SpatialKey(3,3))
    bounds1 intersect bounds2 should be (expected)
    bounds2 intersect bounds1 should be (expected)
  }

  it("checks for inclusion on keys") {
    bounds1 includes SpatialKey(1,1) should be (true)
    bounds1 includes SpatialKey(4,4) should be (false)
    bounds2 includes SpatialKey(1,1) should be (false)
    bounds3 includes SpatialKey(1,1) should be (false)
  }
}
