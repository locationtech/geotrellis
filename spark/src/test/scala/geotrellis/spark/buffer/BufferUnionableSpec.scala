package geotrellis.spark.buffer

import geotrellis.spark._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

object Stuff {
  case class UnionableThing(n: Int) {
    def union(other: Any): UnionableThing = {
      other match {
        case that: UnionableThing => UnionableThing(this.n * that.n)
        case _ => throw new Exception
      }
    }
  }
}

class BufferUnionableSpec extends FunSpec with TestEnvironment {

  import Stuff.UnionableThing

  describe("General BufferTiles functionality") {
    it("should union neighbors, not union non-neighbors") {
      val key1 = SpatialKey(0,0)
      val key2 = SpatialKey(1,1)
      val key3 = SpatialKey(13, 33)
      val thing1 = UnionableThing(47)
      val thing2 = UnionableThing(53)
      val thing3 = UnionableThing(59)

      val rdd = sc.parallelize(List((key1, thing1), (key2, thing2), (key3, thing3)))
      val results = BufferUnionable(rdd).map({ case (k, thing) => k -> thing.n }).collect.toMap

      results(key1) should be (47 * 53)
      results(key2) should be (47 * 53)
      results(key3) should be (59)
    }
  }

}
